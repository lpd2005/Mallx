# MallX 商品图片素材清单

本目录包含 10 组由内置 ImageGen 生成的虚构品牌商品素材。每组共 5 张 PNG：1 个透明品牌标志、1 张商品主图、3 张 SKU 规格图。商品图均采用 1:1 暖白棚拍背景，适配商城商品卡片及详情页的 `contain` 显示方式。

| 目录 | 品牌 | 商品 | 规格图 |
| --- | --- | --- | --- |
| `01-aurora-brew` | AURORA BREW | AB-01 自动意式咖啡机 | 深林绿标准款、奶油白标准款、曜石黑奶泡套装 |
| `02-lumasound` | LUMASOUND | LS-X1 头戴式降噪耳机 | 午夜蓝、雾银灰、晨雾白 |
| `03-verdant-air` | VERDANT AIR | VA-Mist 超声波香薰机 | 鼠尾草绿 200ml、陶土粉 200ml、雾灰蓝 300ml |
| `04-northpeak` | NORTHPEAK | NP-22 城市户外背包 | 松林绿 22L、岩石灰 22L、夜行黑 28L |
| `05-solara` | SOLARA | SL-Arc LED 护眼台灯 | 曜石黑 40cm、月岩银 40cm、深林绿 50cm |
| `06-terrasip` | TERRASIP | TS-500 真空保温杯 | 陶土橙 500ml、苔藓绿 500ml、沙岩白 750ml |
| `07-cloudrest` | CLOUDREST | CR-Wave 记忆棉颈椎枕 | 柔软低枕、支撑中枕、强支撑高枕 |
| `08-purehaven` | PUREHAVEN | PH-360 桌面空气净化器 | 清新白基础滤芯、雾绿除醛滤芯、曜石灰宠物滤芯 |
| `09-novacharge` | NOVACHARGE | NC-10 磁吸充电宝 | 石墨黑 10000mAh、雾银 10000mAh、森林绿 20000mAh |
| `10-kindred-dew` | KINDRED DEW | 植物水润护肤系列 | 水润基础三件套、修护精华双瓶套、丰润五件礼盒装 |

## 文件命名

- `brand-logo.png`：品牌标志，要求透明背景。
- `main.png`：SPU 商品主图。
- `sku-01-*.png`、`sku-02-*.png`、`sku-03-*.png`：SKU 规格图，英文文件名与上表顺序一一对应。

## 生成提示词体系

品牌标志统一使用 `logo-brand` 用例：居中、符号优先、透明背景、虚构英文品牌字标、无水印及无商标符号。

商品主图统一使用 `product-mockup` 用例：高端写实棚拍、暖白 `#f7f9f8` 无缝背景、主体完整居中、约 12% 留白、柔和自然光、细微落地阴影、无可读品牌文字、无水印及无边框。

规格图统一使用 `precise-object-edit` 用例，以对应 `main.png` 为编辑基准；锁定商品结构、相机角度、背景和光影，仅改变 SKU 的颜色、容量、高度、厚度或套装数量。

