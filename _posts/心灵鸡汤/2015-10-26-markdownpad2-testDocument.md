---
layout: post
title: "展示图片"
description: "没事别老看手机"
category: 反省,庸人不自扰
tags: [反省,庸人不自扰]
---

通过git上面，直接的展示，这样我就不需要换markDown
编辑器了：

![](https://github.com/flyBread/flyBread.github.io/blob/master/media/SDS%E5%92%8CCString%E7%9A%84%E6%AF%94%E8%BE%83.PNG?raw=true)

这种方式的图片的存储的地址在git上面，就能够分类的存储自己的图片了，对应在本地的文件的话，就是flyBread.github.io/blob/master/media/
不过需要记住git的网址：https://github.com/flyBread/flyBread.github.io 网址。

不过为什么不能直接的使用绝对路那？难道是自己放置图片的地址不正确，这个也有可能，因为测试显示的URL的路径是：
http://127.0.0.1:4000/2015/10/markdownpad2-testDocument/

所以需要找到：2015/10/markdownpad2-testDocument/ 所以我们可以根据这个来编写绝对的
路径，首先我们试一下放在markdownpad2-testDocument目录下面是否可行

![](./0201-5.png)

不行，因为我们使用的jekyll在生成页面的时候，会直接的把我们放进去的图片删除(可能会直接
连文件夹删除重新建立，需要根据源码进行确认)所以我们再利用绝对路径试一下： 
   
![](../../../../_site/media/pic2014/0201-1.png)   

出来了，但是在CuteMarkEd的编辑的预览界面中竟然没有出来，这就很奇怪了，在这里比较一下
两个绝对路径：     

CuteMarkEd的绝对路径是：../.../../../media/picStore/app95519529.jpg   
git上面的绝对路径是：../../../../media/picStore/app95519529.jpg   

奇怪的多了一个. 下一步我装备在换一个CuteMarkEd安装包，在测试一下
根据测试来看，是可以使用的，我们就能够召唤出来美女了

![](../../../../media/picStore/app95519529.jpg)




