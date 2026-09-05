# MallX 图片上传与OSS配置说明

## 1. 当前方案

上传图片 → 返回完整公开URL字符串 → 前端通过业务接口保存URL → 查询时原样返回并展示。

数据库和Java字段名继续沿用 `avatarOssKey`、`mainImageOssKey`、`imageOssKey`、`iconOssKey`、`logoOssKey`，但现在存的是完整URL，不再是OSS对象Key。购物车展示、订单图片快照和后台查询同样直接使用这个值。

已移除上传结果VO、签名到期时间、两个GET图片地址刷新接口，以及上一版商品VO新增的重复URL字段。前端不再读取 `mainImageUrl` 等新增字段，也不再调用签名接口。

商品图和头像均为公开图片，没有私有签名保护。公开读不等于需要公开写：建议Bucket设置为公共读、授权写，或通过更细粒度授权实现。公共读写会使他人绕过后端权限直接上传、覆盖或删除OSS对象，并可能产生费用。后端的登录校验无法保护公共读写的Bucket。本次代码不修改云端ACL。

## 2. 上传接口

| 用途 | 方法和路径 | 权限 |
| --- | --- | --- |
| 商品、品牌、分类图片 | POST /admin/files/images/upload | 登录、admin:access、product:manage |
| 用户头像 | POST /users/me/files/avatar/upload | 登录 |

原来的POST路径 `/admin/files/images` 和 `/users/me/files/avatar` 也保留，作为同一上传方法的兼容入口。没有新增匿名上传接口。

请求为 `multipart/form-data`，文件字段名为 `file`。成功返回HTTP 200，响应体直接是字符串，例如：

```text
https://images.example.com/2026/09/0123456789abcdef0123456789abcdef.png
```

不是 `Result<String>`，不包含 `data`、`objectKey` 或 `expiresAt`。失败仍由现有全局异常处理返回错误信息，前端先检查HTTP状态。

```javascript
const form = new FormData();
form.append('file', file);
const response = await fetch('/admin/files/images/upload', {
  method: 'POST',
  headers: { Authorization: token },
  body: form
});
if (!response.ok) {
  throw new Error(await response.text());
}
const imageUrl = await response.text();
// 商品编辑表单中的旧字段现在直接存URL。
productForm.mainImageOssKey = imageUrl;
```

不要手动设置multipart的Content-Type，浏览器会生成boundary；上传请求无需指定Accept: application/json。查询商品后直接用 `product.mainImageOssKey` 作为img的src。头像上传后，通过现有 `PUT /users/me` 将URL放入 `avatarOssKey`，并填写该完整资料更新接口其他必填字段。

上传本身不更新业务表，也不删除旧图片；取消编辑可能留下未引用对象。

## 3. 代码结构与保留的校验

- Controller：接收MultipartFile，保留原有鉴权，返回String。
- ImageUploadService：校验图片，然后调用工具类。
- AliyunOssUtil：简单的 `upload(byte[] content, String originalFilename)`，生成日期目录和UUID文件名，上传后返回完整URL。
- AliyunOssConfiguration：保留延迟创建、复用客户端和关闭释放，不再配置临时GET签名。

保留真实JPEG/PNG检测、重新编码、5MB文件上限、6MB请求上限、宽高8192及1600万像素限制，不直接相信文件名或Content-Type。图片解码和重新编码会丢弃原始元数据。新文件统一保存在 `yyyy/MM/uuid.jpg或.png`；旧products、avatars目录中的文件不会移动或删除。

## 4. OSS配置

在本机application-local.yml已有的mall节点中合并配置，不要重复创建同名节点。本次未修改你的本机配置。

```yaml
mall:
  oss:
    endpoint: https://oss-cn-hangzhou.aliyuncs.com
    bucket-name: your-bucket-name
    region: cn-hangzhou
```

上面均为示例，替换成自己的地域、Bucket和Endpoint。不再单独配置图片访问域名，工具类只根据Endpoint生成完整地址：

- 标准Endpoint：返回 `https://Bucket.oss-cn-地域.aliyuncs.com/对象Key`。
- 自定义Endpoint：直接用 `Endpoint/对象Key`，不会重复拼接Bucket。
- 内网Endpoint：不支持，上传前会拒绝；请使用浏览器可访问的公网或已绑定并配置HTTPS的自定义Endpoint。

是否能用默认公网域名取决于OSS账号及地域政策；受限制时配置自定义域名。参见[阿里云域名策略公告](https://www.alibabacloud.com/zh/notice/oss_update_notice_policy_change_in_calling_data_api_operations_via_the_default_public_domain_name_45a)。填写域名不等于自动完成HTTPS、DNS或访问权限配置。

使用环境变量提供授权上传凭证，不能把AccessKey交给前端：

```text
OSS_ACCESS_KEY_ID=RAM用户的AccessKeyId
OSS_ACCESS_KEY_SECRET=RAM用户的AccessKeySecret
```

STS临时凭证还需要 `OSS_SESSION_TOKEN`。上传操作仍使用SDK V4请求签名；取消的是供浏览器读取私有图片的临时URL签名，不是删除上传凭证。RAM账号只授予所需上传目录的PutObject权限，不需要Bucket管理、列表和删除全权限。

旧配置 `public-base-url`、`product-public-base-url`、`signed-url-seconds` 已不再使用。如果本机配置还保留这些项，可以删除；本次未改动本机私有配置文件。

## 5. 数据库升级与旧数据

新建表脚本已把6个图片字段扩容为VARCHAR(1024)，DTO同步限制1024字符，并要求非空图片地址为完整HTTPS URL。字段名不改，避免额外的表字段重命名和接口字段变更。

已有数据库先备份相关表，再执行：

```text
mall-common/src/main/resources/db/migration-image-url-v1.sql
```

该脚本只扩容字段及更新COMMENT，不清空或自动转换数据；MySQL DDL会隐式提交。本次未在你的数据库执行脚本。

旧记录如果仍然是对象Key，不会因为发布代码就自动成为URL。处理方式：

1. 少量数据：重新上传图片，通过业务编辑接口保存返回的URL。
2. 批量迁移：确认每个历史Key对应的Bucket和可访问域名后，为安全的相对Key补上实际域名；已是完整URL的值不要重复拼接。需要同时检查分类、品牌、SPU、SKU、用户头像和订单明细快照。
3. 有中文、空格或特殊字符的Key，应做URL路径编码，不能盲目用SQL CONCAT。迁移后抽样验证匿名访问成功。

直接保存完整URL意味着以后换域名时需要迁移已保存地址；旧订单快照中的URL也不会随配置自动更新。不能将私有签名URL作为长期图片地址保存。

## 6. 验收

上一版简化上传通过编译打包和16项临时内存校验，使用模拟OSS和独立MVC环境，不代表真实云端上传或完整登录链路验收；未新增测试源码或测试依赖。其中独立公开域名分支现已删除，该分支的历史校验不再适用于当前版本。

本版需要确认：

1. 带权限上传JPEG/PNG，响应体为完整URL字符串。
2. 保存商品或头像后查询，原OssKey字段原样返回URL，前端无需额外转换。
3. 游客可读取公开图片，但不能调用上传接口；普通用户不能调用后台上传接口。
4. 错误文件、空文件、缺少file、超限请求被拒绝。
5. 在浏览器无登录状态打开返回URL；代码生成地址并不证明OSS权限和域名已经配置成功。

历史193/209项测试和真实环境异常复测见《MallX_后端真实环境验收报告_20260904》。此前测试源码已按要求删除，本次不会恢复。真实OSS上传和已有数据库迁移仍需单独验收。
