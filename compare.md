## 对比测试结果

与其他库对比: [代码 `PoolBenchmark.java`](src/test/java/cn/itcraft/frogeggs/compare/PoolBenchmark.java)

- [`commons-pool2`](https://commons.apache.org/proper/commons-pool/)
- [`vibur-object-pool`](https://github.com/vibur/vibur-object-pool)
- [`BeeOP`](https://github.com/Chris2018998/BeeOP)
- [`lite-pool`](https://github.com/nextopcn/lite-pool)
- [`generic-object-pool`](https://github.com/bbottema/generic-object-pool)

> 运行于 Intel(R) Core(TM) i5-10210U CPU @ 1.60GHz

> `JVM` 开启参数 `-XX:-RestrictContended`

```verilog
Benchmark                                              Mode  Cnt          Score         Error  Units
PoolBenchmark.testBeeOP                               thrpt   15   45631929.496 ± 3516996.899  ops/s
PoolBenchmark.testBeeOPOneRequestMultiTimes           thrpt   15     150137.646 ±    6733.868  ops/s
PoolBenchmark.testCommonsPool                         thrpt   15    2472299.634 ±   65823.910  ops/s
PoolBenchmark.testCommonsPoolOneRequestMultiTimes     thrpt   15      72447.603 ±    2567.174  ops/s
PoolBenchmark.testGOPool                              thrpt   15    5511393.640 ±  130550.841  ops/s
PoolBenchmark.testGOPoolOneRequestMultiTimes          thrpt   15     176163.376 ±    2585.441  ops/s
PoolBenchmark.testLitePool                            thrpt   15     480685.980 ±   11674.224  ops/s
PoolBenchmark.testLitePoolOneRequestMultiTimes        thrpt   15     477268.366 ±    7905.136  ops/s
PoolBenchmark.testNew                                 thrpt   15  172464240.424 ± 1517871.741  ops/s
PoolBenchmark.testNewOneRequestMultiTimes             thrpt   15    4805402.872 ±   63248.755  ops/s
PoolBenchmark.testTurtleEggsPool                      thrpt   15   69296075.507 ± 1201854.398  ops/s
PoolBenchmark.testTurtleEggsPoolOneRequestMultiTimes  thrpt   15    3854569.738 ±  316905.589  ops/s
PoolBenchmark.testViburPool                           thrpt   15    4657150.711 ±  190528.985  ops/s
PoolBenchmark.testViburPoolOneRequestMultiTimes       thrpt   15     136191.380 ±    2066.646  ops/s
```
