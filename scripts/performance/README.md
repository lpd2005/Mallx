# MallX MVP JMeter压测

## 文件

- `prepare-mallx-pressure-data.sql`：初始化或重置500个压测用户、1万个SPU、2万个SKU及库存和优惠券数据。
- `cleanup-mallx-pressure-data.sql`：按固定压测ID和命名清理上述MySQL测试数据，不影响普通账号和商品。
- `mallx-mvp.jmx`：使用`scenario`参数切换商品、购物车、优惠券和热点库存下单场景。

## 固定数据

| 数据 | 范围/值 |
| --- | --- |
| 用户ID | 30001～30500 |
| 手机号 | 18810000000～18810000499 |
| 密码 | 123456 |
| 地址ID | 40001～40500，与线程号一一对应 |
| SPU | 50001 |
| 普通SKU | 60001，可用库存1000000 |
| 热点SKU | 60002，可用库存100 |
| 限量优惠券模板 | 70001，总库存100 |
| 商品目录分类 | 51001～51050，共50个 |
| 商品目录品牌 | 52001～52050，共50个 |
| 商品目录SPU | 800001～810000，共10000个；90%上架、5%草稿、5%下架 |
| 商品目录SKU | 3000001～3020000，共20000个；每个SPU两个SKU |

脚本仅用于独立压测数据库。每轮优惠券或下单压测前重新执行SQL，可删除上一轮压测交易并恢复库存。它只处理上述固定范围，不处理普通业务账号。

## 运行前

1. 启动MySQL、Redis和MallX，确认`GET /products?current=1&size=1`返回200。
2. 压测配置中关闭`StdOutImpl` SQL输出，使用`NoLoggingImpl`，根日志建议设为WARN。
3. 执行`prepare-mallx-pressure-data.sql`。本仓库当前数据库已初始化一次。
4. 真实压测使用命令行非GUI模式。结果目录必须不存在，否则JMeter拒绝生成报告。

本机JMeter入口：

```text
D:\JMeter\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat
```

以下命令均在`D:\MallX\Mallx`目录执行。服务端口不是8080时修改`-Jport`。

## 1. 商品列表基线

先从小流量开始：

```powershell
& 'D:\JMeter\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat' `
  -n -t scripts/performance/mallx-mvp.jmx `
  -Jscenario=product -Jthreads=50 -Jramp=30 -Jloops=20 -Jport=8080 `
  -l target/jmeter-product.jtl -e -o target/jmeter-product-report
```

这个场景不登录，每个线程循环查询商品列表。请求总量约为`threads × loops`。默认查询第1页、每页20条，可以通过`-Jcurrent`和`-Jsize`修改分页参数。

除了默认首页，还应分别测试深分页、分类过滤、品牌过滤和关键字查询。例如：

```powershell
# 第400页；结果约落在上架商品的后半段
-Jscenario=product -Jcurrent=400 -Jsize=20

# 单个分类或品牌过滤
-Jscenario=product -JcategoryId=51001
-Jscenario=product -JbrandId=52001

# 商品名LIKE查询
-Jscenario=product -Jkeyword=商品-00010
```

以上参数追加在完整JMeter命令中；未提供的过滤参数会作为空值，由后端按“无过滤条件”处理。

## 2. 购物车查询

```powershell
& 'D:\JMeter\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat' `
  -n -t scripts/performance/mallx-mvp.jmx `
  -Jscenario=cart -Jthreads=100 -Jramp=30 -Jloops=20 -Jport=8080 `
  -l target/jmeter-cart.jtl -e -o target/jmeter-cart-report
```

每个线程使用不同账号，只登录一次，先清理并添加普通SKU，然后循环查询购物车。报告中登录和准备请求应与`购物车查询`分开观察。

## 3. 500用户并发领取100张券

每轮前必须重新执行数据SQL：

```powershell
mysql --user=root --password --database=mall `
  --execute="source D:/MallX/Mallx/scripts/performance/prepare-mallx-pressure-data.sql"

& 'D:\JMeter\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat' `
  -n -t scripts/performance/mallx-mvp.jmx `
  -Jscenario=coupon -Jthreads=500 -Jramp=10 -Jloops=1 -Jport=8080 `
  -l target/jmeter-coupon.jtl -e -o target/jmeter-coupon-report
```

所有线程先完成登录，再在领取请求前通过集合点同时释放，确保测到真正的并发争抢，而不是被登录耗时打散。`优惠券已领完`会被标记为预期拒绝，不计为系统错误，并改为独立Sample名称。`syncUsers`默认为当前线程组全部线程，`syncTimeoutMs`默认120000毫秒。验证SQL：

```sql
SELECT total_stock, received_stock FROM coupon_template WHERE id = 70001;
SELECT COUNT(*) FROM coupon_user WHERE template_id = 70001;
SELECT user_id, COUNT(*) count
FROM coupon_user WHERE template_id = 70001
GROUP BY user_id HAVING count > 1;
```

期望：`received_stock=100`、用户券数量100、重复用户查询为空。

## 4. 500用户争抢100件热点库存

每轮前重新执行数据SQL，然后运行：

```powershell
& 'D:\JMeter\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat' `
  -n -t scripts/performance/mallx-mvp.jmx `
  -Jscenario=order -Jthreads=500 -Jramp=10 -Jloops=1 -Jport=8080 `
  -l target/jmeter-order.jtl -e -o target/jmeter-order-report
```

每线程登录独立用户，先清理普通SKU和热点SKU两个测试购物车项，再重新添加一件热点SKU；所有线程准备完成后在创建订单请求前通过集合点同时释放，再创建一次订单。`库存不足`和购物车复核阶段的`商品已无库存`都会作为预期拒绝单独统计。该场景只创建订单并锁库存，不模拟支付，期望成功100单。

```sql
SELECT available_stock, locked_stock, sold_stock
FROM inventory_stock WHERE sku_id = 60002;

SELECT COUNT(*) success_orders
FROM mall_order
WHERE user_id BETWEEN 30001 AND 30500 AND status = 1;

SELECT business_no, sku_id, operation_type, COUNT(*) count
FROM inventory_operation
WHERE sku_id = 60002
GROUP BY business_no, sku_id, operation_type
HAVING count > 1;
```

期望：`available_stock=0`、`locked_stock=100`、`sold_stock=0`、待支付订单100条、重复流水查询为空。

## 5. 100个成功订单并发模拟支付

该场景会完整重跑“500人争抢100件库存”，提取100个成功订单并创建支付单，随后在集合点同时释放100个模拟支付请求。每个成功支付还会重复通知一次，用于验证支付和库存扣减幂等。

每轮前重新执行数据SQL，然后运行：

```powershell
& 'D:\JMeter\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat' `
  -n -t scripts/performance/mallx-mvp.jmx `
  -Jscenario=payment -Jthreads=500 -Jramp=30 -Jloops=1 `
  -JsyncUsers=500 -JpaymentSyncUsers=100 -Jport=8080 `
  -l target/jmeter-payment.jtl -e -o target/jmeter-payment-report
```

期望：成功订单100、支付记录100且全部已支付、订单100个进入已支付状态；热点SKU应为`available_stock=0`、`locked_stock=0`、`sold_stock=100`，扣减流水应恰好100条且没有重复。

## 6. 500用户并发秒杀100件库存

先执行数据SQL，再通过后台接口创建包含SKU `60002`、资格库存100、每人限购1件的活动并执行预热。取得返回的秒杀商品ID后运行：

```powershell
& 'D:\JMeter\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat' `
  -n -t scripts/performance/mallx-mvp.jmx `
  -Jscenario=seckill -Jthreads=500 -Jramp=10 -Jloops=1 `
  -JsyncUsers=500 -JseckillItemId=<秒杀商品ID> -Jport=8080 `
  -l target/jmeter-seckill/result.jtl -e -o target/jmeter-seckill/html
```

所有线程登录完成后在抢购接口前集合并同时释放。100个资格预扣成功、400个“秒杀商品已售罄”属于正确结果；后者会被标记为预期拒绝，不计为系统错误。压测后还必须等待异步消费收敛，并联合检查：

```sql
SELECT status, COUNT(*) FROM seckill_order_request
WHERE activity_id = <活动ID> GROUP BY status;

SELECT total_stock, sold_stock FROM seckill_item WHERE id = <秒杀商品ID>;
SELECT available_stock, locked_stock, sold_stock
FROM inventory_stock WHERE sku_id = 60002;
```

正常期望为成功请求100、正式订单100、活动销量100，库存为`0/100/0`（订单尚未支付）。同时应确认Redis资格库存为0、待处理计数和ZSet归零，RabbitMQ业务队列及死信队列均为空。

验证异步削峰时，可仅在测试进程中设置`spring.rabbitmq.listener.simple.auto-startup=false`关闭消费者：抢购结束后应看到RabbitMQ积压100条、MySQL订单仍为0；随后恢复消费者，应准确生成100单并清空队列。该操作只用于测试，不能作为生产配置。

## 参数与限制

- `scenario`：`product`、`cart`、`coupon`、`order`、`payment`、`seckill`。
- `threads`：并发线程数；认证场景最大500，因为只准备了500个账号。
- `ramp`：全部线程启动所用秒数，不能一开始就设为0。
- `loops`：商品和购物车可大于1；优惠券和下单由Once Only控制器保证每用户只执行一次。
- `port`：MallX端口，默认8080。
- `current`、`size`：商品列表页码和每页数量，默认1和20。
- `categoryId`、`brandId`、`keyword`：商品列表可选过滤条件。
- `productPath`：商品查询路径，默认`/products`；升级版搜索对比时传`/search/products`。
- `seckillItemId`：秒杀场景必填，为后台创建并预热后返回的秒杀商品ID。
- 默认主机为`127.0.0.1`。如需远程压测机，可在JMX的`host`默认值中修改。

不要并发执行coupon和order两个场景，也不要在没有重置数据时重复比较结果。压测机和服务端在同一台电脑时，结果只能作为本机基线，不能直接宣传为生产承载能力。

## 清理压测数据

完成压测后，可在项目根目录执行：

```powershell
mysql --user=root --password --database=mall `
  --execute="source D:/MallX/Mallx/scripts/performance/cleanup-mallx-pressure-data.sql"
```

该SQL只清理MySQL中的固定压测数据。购物车存放在Redis中，键为`mall:cart:{30001}`至`mall:cart:{30500}`，不属于SQL清理范围；JMeter生成的JTL和HTML报告也会保留。
