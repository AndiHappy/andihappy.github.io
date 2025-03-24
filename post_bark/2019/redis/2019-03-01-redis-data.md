---
layout: post
title: "redis梳理"
subtitle: "redis对应的数据结构底层实现的原理，以及常见的问题"
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true
tags: redis  
---
redis主要操作的数据有五种，String，List，Map，Set，sort Set 简称为ZSet。这五类的操作的数据的底层
是如何运行的。
redis为什么这么快？
redis的集群的模式
redis的内存管理模式
redis的持久化操作等常见的问题。

<!--more-->

首先我们发现问题，然后我们一个一个的回答，深入。
