---
layout: post
title: "为什么RethinkDB失败了？"
---
文件地址：http://www.defmacro.org/2017/01/18/why-rethinkdb-failed.html

In hindsight, two things went wrong – we picked a terrible market and optimized the product for the wrong metrics of goodness.Each mistake likely cut RethinkDB’s valuation by one to two orders of magnitude（数量级）. So if we got either of these right, RethinkDB would have been the size of MongoDB, and if we got both of them right, we eventually could have been the size of Red Hat[1].

<!--more-->

两个原因：

市场的选择：
		设想中的数据库市场非常的大，但是大家把你当做是开源软件。需求很大，但很多都是伪需求。伪需求的前提下面，真正的订单获取非常的困难。
		Developers love building developer tools, often for free. So while there is massive demand, the supply vastly outstrips it. This drives the number of alternatives up, and the prices down to zero.
For us, it meant an intractable customer acquisition funnel. This has disastrous domino effects. It demoralizes the team, and makes it very challenging to attract investment and hire top talent. In turn, that constrains your resources so you can’t make sufficient investment in product and distribution. Startups live and die by momentum, and early distribution challenges almost always doom you to eventual death.

产品的方向：

一开始的产品的方向: elegant, robust, and beautiful product

* Correctness. We made very strict guarantees, and fulfilled them religiously.
* Simplicity of the interface. We took on most of the complexity in the implementation, so application developers wouldn’t have to.
* Consistency. We made everything from the query language, to the client drivers, to cluster configuration, to documentation, to the marketing copy on the front page as consistent as possible.

其分析的正确的方向：

* Timely arrival. They wanted the product to actually exist when they needed it, not three years later.
* Palpable speed. People wanted RethinkDB to be fast on workloads they actually tried, rather than “real world” workloads we suggested. For example, they’d write quick scripts to measure how long it takes to insert ten thousand documents without ever reading them back. MongoDB mastered these workloads brilliantly, while we fought the losing battle of educating the market.
* A use case. We set out to build a good database system, but users wanted a good way to do X(e.g. a good way to store JSON documents from hapi, a good way to store and analyze logs, a good way to create reports, etc.)

合适的就是最好的，这个是完美的注解。

By the time we felt RethinkDB satisfied our design goals and we were confident enough to recommend it to be used in production, almost everyone was asking “how is RethinkDB different from MongoDB?” We worked hard to explain why correctness, simplicity, and consistency are important, but ultimately these weren’t the metrics of goodness that mattered to most users.

迭代解决了很多的问题，满足需求，满足当下的需求，满足当下的有效的需求。

But over time I learned to appreciate the wisdom of the crowds. MongoDB turned regular developers into heroes when people needed it, not years after the fact. It made data storage fast, and let people ship products quickly. And over time, MongoDB grew up. One by one, they fixed the issues with the architecture, and now it is an excellent product. It may not be as beautiful as we would have wanted, but it does the job, and it does it well.


一步错步步错，在原有的基础性上进行了错误的方向转变。
The obvious problem with a small database company building a cloud service is that it pattern matches to a common startup failure mode – splitting focus. 

规律就是规律，不会因为你的忽视就不存在。
Early RethinkDB was quite a bit like that. We had no intuition for products or markets, so we’d go through the motions of building a company without actually understanding what we were doing. What’s more, we had enormous optimism bias. Just like physicians know that gifts from pharmaceutical companies have biasing effects for other physicians but believe they are immune from the effect, we believed we were immune from the laws of economics and the math of running a business. The math, of course, eventually caught up with us.
