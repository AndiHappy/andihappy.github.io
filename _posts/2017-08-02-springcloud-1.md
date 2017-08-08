---          
layout:     post
title:      "activiti和设计模式(1)"
subtitle:   "通过学习activiti的源码，从设计模式这个角度去分析这个框架，熟悉源码，学习其中的设计思想"
date:       2017-08-02 12:00:00
author:     "zhailzh"
header-img: "img/2017-07-27-begine.md"
catalog:     true
tags:     
- 学习
categories:    
- activiti, desigen pattern         
---   

## activiti 源码和设计模式（1）          			
1. activiti的源码的主要的架构
2. activiti前两层包含的设计模式

从我的工作的经历以及了解到的内容，发送activiti可谓是一个扩展性极好的框架。     
一个原因是因为工作流本身的业务复杂性本身要求activiti实现的灵活性，复杂的业务会对整个框架的扩展提出了极高的要求，如果扩展性不够好，复杂的业务可以把二次开发的人员直接逼疯！！        

另外的一个原因，在于activiti的主设计人员已经经历了一次设计jbpmn，可能被坑过一次了，所以在第二次设计的时候，更加的完善。    

至于为什么选择了从设计模式分析activiti，主要是想提供一个另外的角度，看看能不能学到点大神设计框架的精髓。

<!--more-->
