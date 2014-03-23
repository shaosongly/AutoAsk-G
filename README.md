AutoAsk-G
=========

问答系统问题转换程序


##数据库设计

本研究的目的是构建一个汽车领域的智能问答系统，用于汽车销售，因此，如何较好的组织存储汽车领域知识是其中一项非常重要的内容。为了能够快速处理大量数据，实现用户问答，考虑使用数据库来存储部分领域知识。

1. 整体结构

 数据库的设计应该从实际应用角度出发，涵盖用户买车过程中可能遇到的各种问题。通过查阅资料和调研许多汽车网站，我们将相关信息分为汽车参数信息、配置信息、评价信息和经销商信息4个组成部分。数据库的整体结构如下图所示：
 
  ![整体结构](http://github.com/shaosongly/AutoAsk-G/raw/master/pic/db1.png)
  
2. 具体实现
 
 基于上述结构，我们建立如下数据表：

 ![tables](http://github.com/shaosongly/AutoAsk-G/raw/master/pic/db2.png)
 
 auto_base是主表，包含了汽车品牌、车型、级别和价格等基本信息，以汽车ID作为主键，和其他各表关联，可以查询到汽车各部件的参数和配置以及评价指标，为用户选购提供指导。经销商表会描述经销商的公司名称、经营范围、地址、电话和类型等内容，方便用户选择合适的购买方式。

 对于表中各字段的具体设计：
 
 参数类表格：表的结构相对固定，各个字段不会发生变化（增加、删除）
 
 配置类表格：每一类配置的具体内容会发生变化，不应该将具体属性设置为表的字段
，应该以key1:value1,key2:value2,...,keyn:valuen的形式存储具体内容

 字段取值类型：
 
 - 数字
 
 - 整型、浮点型
 
 - 字符串 
 
    - 单一字符串，如auto_level：中型车   
 
    - 多个字符串，如color：银石, 赫雷斯黑, 黑色, 墨尔本红, 太空灰, 雪山白, 因特拉格斯蓝
 
    - 含有格式信息的字符串，如external：电动天窗:无,全景天窗:标配,运动外观套件:标配
 
    - 表示数值的范围，如luggage_compartment：206-305  
 
  说明：最后一种情况当做单一字符串处理，需要进行条件判断，求最大、最小或者均值等操作的字段类型应设置为数字。
  
  实验环境使用ACCESS数据库进行实现，但考虑到速度、并发性和数据存储量等问题，实际应用的开发应该使用MySQL数据库，以获取更好的性能。
  
**********************************************************************

##程序实现
    
1. 基本流程

 1）将传入的问题参数解析成(key:value)对的形式，存入合适的数据结构，并判断问题参数是否符合规则；

 2）根据解析的参数，组装SQL语句，查询数据库，获取结果；
 
 3）根据问题和查询到的结果，组织答案返回给用户。
 
2. 预处理模块设计

 输入：基本形式--------参数名1:参数值1,参数名2:参数值2,……,参数名k:参数值k 
 1) 参数值类型：
 - 单一取值（字符串、数字）；
 - 包含多个值，表示为：(参数值1;参数值2;…;参数值m)；
 - 带有条件： >、<、>=、<=、[]；
 - ?、min、max、avg、num等符号表示要查询的参数值；

 e.g. 品牌:奔驰,级别:中型车,车体结构:(三厢车;SUV),价格:？
 
 2) 问题解析：

 先将问题按照格式解析成Map<String,ArrayList<String>>集合params和querys，结构类似下面格式：
 
 key1	value1
 
 key2	value2
 
 keym	valuem

 其中params存储非查询参数，querys存储待查询参数。
 
 3）参数转换和异常判断
 
 对于params中每一个value数组，如果含有多个带有条件的值，值的数目超过2，认为是异常，然后判断两个值能否构成区间，若不能，是异常，否则将其转化成[]形式。
 
 如果value在数据库中是按照特定格式存储的，需要将value转换成合适的格式。
例如查询的key:value是电动天窗:标配，需要将value由标配转换成电动天窗:标配。

 如果value数组只有一个值，不作处理；存在value数组含有两个值，则将所有只含有一个值的value数组填充为含有两个相同的值，这相当于将参数分成两组，出现1次的值两组共享，出现2次的分别分到两个组，转换成前一种情况。存在value数组含有超过两个值，认为传入参数异常，无法处理，返回错误信息。
 
 对于querys集合，若为空，认为这是一个判断问题，没有要查询的参数，根据能否查询到数据返回是或者否；存在一个key，检查value数组，若都含有两个值，则认为查询主体有两个，对于每组值分别和待查询参数组合进行查询；存在多个key，有多个要查询的参数，对于每一个参数分别进行查询和存储结果。  
 
 输出：params和querys集合
 
3. 查询模块设计

 输入：params和querys集合

 1）查询思路

 若querys集合为空：先将params中的属性按照表格分组，然后每张表生成一个sql语句，查询满足条件的汽车ID，然后对结果求交集，求得满足所有条件的ID。（如果问题带有全称量词---满足A的都满足B吗，应该先根据量词的修饰范围将params集合分成A和B两部分，分别查询，然后判断A和B的结果是否一致）

 若querys集合不为空：如果params为空或者params中的属性都和要查询的属性在一张表，那么直接生成SQL语句进行查询；否则，将属性分组，先查询到满足所有条件的汽车ID，然后根据汽车ID，对于querys中的每一个属性，生成SQL语句，查询属性取值。
 
 2）生成一条查询语句：

 ```
 String sql= "select " +target+" from "+tableName+ " where 1=1";
 sql=sql+一个或多个限定条件
 限定条件：
 and propName='value'                      字符串
 and propName like '%%value%%'             部分匹配字符串
 and propName=value                        数字
 and propName <=value                      条件 
 and propName between v1 and v2            区间[v1;v2]
 and p1 like '%%v1%%'  or p2 like '%%v2%%' or p3 like '%%v3%%'   
 and propName in ('v1','v2','v3')          多个取值(v1;v2;v3)
 ```

 配置文件中会定义每一个propName的数据类型以及是否使用like查询(对于文字，可以都使用like查询，以简化程序)
 
 3）结果存储：
 List<Map<String,List>> results
 
 results的外层list中的每一个元素表示一个主体的查询结果，对于每一个主体，根据不同的属性值，又可能有多个查询结果。
 
 结果示例1：

 auto_id：[id2,id3,id5]
 
 auto_id：[id2,id4]     

 结果示例2：
 
 price：[10,20,25,30]

 auto_level：[大型车，小型车，SUV]
 
 输出：results集合
 
4. 问答模块设计

 输入：results和querys集合

 1）若querys为空，没有要查询的内容，问题是一个判断，检查每个主体的结果是否为空，然后分别作出回答。

 如何用自然语言回答问题：
 
 A有没有.....?       A有...../A没有......  
 
 A和B都有.....?     不，A有....，但B没有....  /A和B都有

 将问题拆分出主体、谓语和结论，根据问题答案，用肯定或否定形式组织句子

 2）若querys不为空，对于每一个要查询的内容，分别给出答案。
根据querys中属性对应的value确定如何对数据进行处理
 - ?  对于文字，统计个数，列出结果（过滤掉重复的内容）,对于数值，给出变化区间和均值
 - min   求最小值
 - max  求最大值
 - avg   求平均值
 - num  统计个数

 3）关于比较：问题中有一些含有比较含义的词语，例如好、贵等等，可以根据这些词语，判断是否要对两个主体的结果进行进一步的对比。
 
 - 单一数值：直接进行对比
 - 范围数值：比较平均值
 - 文字：比较个数
  
 输出：自然语言回答



