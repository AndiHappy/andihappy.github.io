---
layout: post
title: "树形数据结构"
subtitle: "认知问题"
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true
tags: JDK  
---
B+，B树
<!--more-->

The image below helps show the differences between B+ trees and B trees.
Advantages of B+ trees:
* Because B+ trees don't have data associated with interior nodes, more keys can fit on a page of memory. Therefore, it will require fewer cache misses in order to access data that is on a leaf node.
* The leaf nodes of B+ trees are linked, so doing a full scan of all objects in a tree requires just one linear pass through all the leaf nodes. A B tree, on the other hand, would require a traversal of every level in the tree. This full-tree traversal will likely involve more cache misses than the linear traversal of B+ leaves.
Advantage of B trees:
* Because B trees contain data with each key, frequently accessed nodes can lie closer to the root, and therefore can be accessed more quickly.
