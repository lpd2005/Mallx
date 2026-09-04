# MallX 图片上传与OSS配置说明

## 1. 本次实现

采用后端转传：前端发送multipart文件，后端校验并重新编码，再上传阿里云OSS。暂不实现前端直传、STS自动刷新、文件管理表、自动删除或图片审核平台。

- `mall-common/oss`：客户端配置、图片校验、目录隔离、上传结果。
- `AliyunOssUtil`：调用OSS上传、生成访问链接；OSS客户端由Spring延迟创建并复用，应用关闭时统一shutdown。
- `mall-admin/AdminFileController`：商品、品牌、分类图片入口，要求`admin:access`与`product:manage`。
- `mall-user/UserFileController`：登录用户头像入口。

所有上传及刷新链接接口都没有`@Public`。游客能浏览商品，不代表可以上传文件或为任意对象签名。

## 2. 接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| POST | /admin/files/images | 上传商品、品牌、分类图片 |
| GET | /admin/files/images/url?objectKey=... | 管理员刷新商品图片地址 |
| POST | /users/me/files/avatar | 上传当前用户头像 |
| GET | /users/me/files/avatar/url?objectKey=... | 刷新当前用户自己上传头像的地址 |

上传类型为`multipart/form-data`，文件字段名称必须为`file`，同时携带登录返回的Token请求头（当前配置为`Authorization`）。不传用户ID、对象Key或上传目录，后端根据登录身份生成。

成功响应示例（Key、签名URL和时间仅示意）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "objectKey": "avatars/100/2026/09/0123456789abcdef0123456789abcdef.png",
    "url": "https://images.example.com/avatars/100/2026/09/0123456789abcdef0123456789abcdef.png?签名参数",
    "expiresAt": "2026-09-04T10:15:00Z"
  }
}
```

业务表只保存`objectKey`，不能保存临时签名URL。头像上传后，将Key传给现有`PUT /users/me`的`avatarOssKey`字段；该接口为完整资料更新，还需填写原有其他必填字段。商品、品牌、分类通过各自管理接口保存对应`*OssKey`字段。

上传本身不自动修改资料，不删除旧图片；取消编辑可能留下未引用文件。本期不做对象删除，后续需按业务引用关系设计清理机制，不能直接给整个图片目录配置短期过期删除。

## 3. 图片校验和目录规则

- 只支持JPEG、PNG，暂不支持GIF、WebP、SVG。
- 原文件与重新编码后的文件均不超过5MB；整个multipart请求不超过6MB。
- 宽高各不超过8192，总像素不超过1600万。先检查尺寸再解码，降低图片解压导致的内存风险。
- 读取文件流也有大小上限，不仅依赖前端声明的文件大小。
- 根据实际图片内容检测格式，不相信原始文件名和请求Content-Type。
- 解码并重新编码后上传，丢弃尾随数据和原始元数据。JPEG可能再次有损压缩；不保留EXIF方向等元数据，前端应先校正手机照片方向。
- 后端生成UUID文件名，并明确设置OSS Content-Type和Content-Length。

目录格式：

```text
products/{管理员ID}/yyyy/MM/{uuid}.jpg或.png
avatars/{用户ID}/yyyy/MM/{uuid}.jpg或.png
```

头像刷新接口只允许当前用户目录；后台刷新接口只允许商品目录。Key需匹配完整格式，不接受完整URL、路径穿越或查询参数。管理员可管理商品目录下其他管理员上传的图片，但不能通过商品接口签发用户头像。

刷新接口只生成链接，不执行HeadObject，因此不证明对象一定存在。新接口也不为原先任意格式的历史Key签名，历史文件需另行规划兼容策略。

## 4. 本地配置

不要提交AccessKey，实际私密配置仍由你在`mallx-application/src/main/resources/application-local.yml`维护。本次没有改动该文件。

在已有`mall`节点中合并下面配置，不要重复创建同名顶层节点：

```yaml
mall:
  oss:
    # 示例：需要事先在OSS绑定该域名并配置HTTPS证书，不能原样使用示例域名。
    endpoint: https://images.example.com
    bucket-name: your-bucket-name
    region: cn-hangzhou
    # 留空：默认私有Bucket，返回限时GET签名链接。
    public-base-url: ""
    signed-url-seconds: 900
```

`application.yml`将`mall.oss.*`映射到`mallx.aliyun.oss.*`，对应`AliyunOssProperties`。Endpoint和Bucket地域必须一致；Endpoint必须包含HTTPS、不能包含Bucket对象路径、查询参数或末尾斜杠。

若账号允许使用标准公网域名，可用`https://oss-cn-hangzhou.aliyuncs.com`，不要手工添加Bucket前缀。注意：自2025年3月20日起新开通OSS服务的用户，其中国内地Bucket通过默认公网域名调用数据API会受限，应使用自定义域名；此前已开通用户与内网域名不受该公告影响。参见[阿里云域名策略公告](https://www.alibabacloud.com/zh/notice/oss_update_notice_policy_change_in_calling_data_api_operations_via_the_default_public_domain_name_45a)。

默认签名预览需要浏览器可访问的公网或自定义Endpoint。如果用内网Endpoint上传，必须另行配置已可公开访问的`public-base-url`，不能把内网签名链接返回给公网浏览器。

### 凭证

在IDEA实际启动配置的Environment variables或部署进程环境中设置：

```text
OSS_ACCESS_KEY_ID=RAM用户的AccessKeyId
OSS_ACCESS_KEY_SECRET=RAM用户的AccessKeySecret
```

SDK读取进程环境变量，不是Spring配置项，也不是`-D`参数。设置后重新启动应用。使用STS临时凭证时还需`OSS_SESSION_TOKEN`；当前环境变量提供器不自动续期，实际URL也可能随STS凭证提前失效。

使用专用RAM用户，上传授予目标目录`oss:PutObject`；私有链接成功读取还需`oss:GetObject`。当前不需要ListBucket、DeleteObject或管理员全权限。RAM资源范围应与上述products、avatars目录一致。不要把Bucket改为公共读写。

SDK配置了连接超时5秒、读写超时15秒、连接池等待5秒、最大连接数50，并使用V4签名。未配置OSS不会阻止其他模块启动，实际上传/刷新时才校验。

### 公开图片或CDN模式

若已配置好允许访问的图片域名或CDN，可填写：

```yaml
mall:
  oss:
    public-base-url: https://cdn.example.com
```

此模式直接返回`public-base-url/objectKey`，`expiresAt`为null，不再生成签名，也不会自动更改Bucket ACL或配置CDN。私有Bucket需由你配置CDN私有回源等访问机制；仅填写域名不等于获得访问权限。此配置同时影响商品图和头像，使用前要确认均适合公开展示。

现有商品、品牌、分类及资料VO仍保留`*OssKey`字段，没有改成持久化URL。前台展示公开图片可使用配置好的图片域名拼接Key；私有商品图目前仅提供后台预览刷新，不提供游客任意Key签名接口。若后续选择让游客查看私有商品图，应在商品查询中校验上架状态后生成展示链接，不能直接公开后台签名接口。

## 5. 前端调用示例

```javascript
const form = new FormData();
form.append('file', fileInput.files[0]);
const response = await fetch('/users/me/files/avatar', {
  method: 'POST',
  headers: { Authorization: token },
  body: form
});
const result = await response.json();
// 检查HTTP状态及result.code后，使用data.url预览，data.objectKey保存资料。
```

不要手动填写multipart的Content-Type，浏览器会自动生成boundary。此模式是后端调用OSS，不要求为文件上传配置OSS浏览器跨域规则；前后端不同源时仍需处理应用接口本身的CORS。

## 6. 验收与当前限制

2026-09-04异常响应修复补充：不支持的上传Content-Type返回HTTP 415；文件超限、缺少file或损坏multipart保持HTTP 400。Tomcat的 `max-swallow-size` 配置为8MB，用于在拒绝上传后有限排空请求体，让6MB附近的超限请求仍能收到完整JSON错误；实际许可仍是单文件5MB、总请求6MB，不是放宽上传大小，也不保证任意超大请求都能收到JSON。

本次新增真实Tomcat HTTP回归后，全量209项测试通过，并使用完整后端及.NET客户端完成12项实际接口复测。详情见《MallX_后端真实环境验收报告_20260904》第7节；OSS真实上传尚未验证。

首次上传功能开发时，执行 `mvn -pl mallx-application -am test -q`，全量193项测试通过，0失败、0错误、0跳过。该阶段测试覆盖JPEG/PNG真实解码、伪装和损坏图片、文件/像素上限、Key目录隔离、上传权限、缺失参数、统一异常响应、客户端延迟初始化，以及假凭证下SDK标准域名/CNAME的本地V4签名。

代码测试不连接真实OSS，不证明账号权限、域名证书、Bucket地域及实际上传已经联调通过。配置完成后依次验证：

1. 正常JPEG/PNG上传成功，OSS对象Key、Content-Type正确，返回链接能预览。
2. Key保存到头像或商品字段后重新查询仍存在；签名过期后刷新链接可访问。
3. 空文件、伪装图片、损坏图片、超过5MB或像素上限的图片被拒绝。
4. 游客不能上传；普通用户不能上传商品图；用户不能刷新其他用户头像。
5. 模拟错误凭证/域名时返回统一500，不返回SDK内部详情；上传超限和缺失参数返回400。

这版没有分布式限流、存储配额、内容审核、病毒扫描或大规模并发测试。正式对公网开放前应补用户上传频率/配额限制，避免登录用户滥用存储和带宽。
