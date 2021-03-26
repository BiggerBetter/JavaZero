# 每日一坑

* Arrays.asList("1","2")返回的是List抽象类不能自动转换为ArrayList，得用new ArrayList<>()构造函数