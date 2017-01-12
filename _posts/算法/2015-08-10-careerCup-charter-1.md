---
layout: post
title: "算法-第一章"
description: "没事别老看手机"
category: 算法
tags: [java,算法]
---

1.1  Implement an algorithm to determine if a string has all unique characters  What if you
can not use additional data structures?

```
// 采用如果是针对26个英文字母的话，不适用其他的数据结构，我们可以使用位操作来搞定
  public boolean isUnique(String value) {

    if (value.length() > 26) {
      return false;
    }

    for (int i = 0; i < value.length(); i++) {
      int letter = value.charAt(i);
      long temp = flag & (1 << (letter - 'a'));
      if (temp == 0) {
        flag = flag | (1 << (letter - 'a'));
      } else {
        return false;
      }
    }

    return true;

  }

```