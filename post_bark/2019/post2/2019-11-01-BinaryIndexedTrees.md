---
layout: post
title: "Binary Indexed Trees 的介绍和学习"
subtitle: "A Fenwick tree or binary indexed tree is a data structure that can efficiently update elements and calculate prefix sums in a table of numbers."
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true
tags: LeetCode  
---

binary indexed tree  一般翻译为树状数组，非常的好奇树状的数组是一个什么样式的？
<!--more-->

#### 介绍

我们常常使用一些数据结构使我们的算法更加的快，binary index tree 就是这样的一个数据结构。发明人为：Peter M. Fenwick 。该数据结构最早主要用于数据的压缩，在算法中，它通常用于**存储频率和操纵累积频率表**。 我们将通过实例来说明该数据结构。

#### 数据结构使用的场景

考虑一个数组a：【1，4，5，3，7，6，9，8，0，2】我们需要如下的两个操作：

1. 修改数组i的值，例如设置a[1] = 10, 表示为：modify(int i,int value) 
2. 计算数组下标i到j的总和，例如计算a[2]+a[3]+a[4]+a[5] 的值，表示为：sum(int i,int j) 或者直接是sum(j) 表示从0到j

我们的目标就是实现这两个功能。

一般的操作来说，对于1，我们可以轻松的实现O(1)的时间复杂度，因为数组支持随机访问。 对于2，我们可以使用遍历的方法实现O(n)的时间复杂度。如果我们做了m次操作，最坏的情况下，都是操作2，那么时间复杂度就是O(m*n),但是利用一些数据结构，我们可以把最坏的情况，优化到O(m*log(n))。

Note：针对上面的情况下，我们也能临时申请一个数组b，存储的是b[i] = (a[1]+a[2]+...+a[i]),这样的话，操作2的时间复杂度就会变为O(1),但是我们在修改a[1]的值后，对应的修改数组b的值的变动就会变为O（n）。所以我们需要一个折中的数据结构！

例如：RMQ 或者 BIT，其中BIT更加的容易实现，并且要求的空间会比RMQ要小，所以我们首先说明binary index tree。后面如果有机会，我们会学习RMQ数据结构。

#### 符号解释

再开始说明这个数据结构之前，我们首先说明一下约定的符合。

BIT：Binary indexed Tree

MaxIdx:源数组，也就是给定数组最大的下标值,等于数组的长度

f[i] : 源数组

c[i] : 简单的Sum，中间数组，c[i]=f[1] + f[2] + ... + f[i]，把f数组中的树加起来。

tree[i] : 就是BIT数组中的i对应的值，标识一些数据的和。这个就是折中的数据结构。

num` : 标识num的反码（每一个二进制取反（1变为0，0变为1）的正数）

NOTE: We set f[0] = 0, c[0] = 0, tree[0] = 0, so sometimes we will ignore index 0.

#### 基本思想

每一个整数都可以标识为二进制，也就是说n可以标识为2的0次方+2的1次方+....+2的k次方的样式，实际上就是使用2进制进行表示。这就意味着：累积频率可以表示为子频率集的总和。也就是说n，可以用2的{0,1,....k}频次来进行表示。在我们的例子中，我们的集合中的元素，使用的是一些连续的非重叠的元素构成的。具体的解释如下：

令idx为BIT的索引。
令r为二进制表示法中最后一个非零数字在idx中的位置，即r为idx的最低有效非零位的位置。 tree [idx]就是索引（idx-2 ^ r +1）到idx（包括端值）的源数组f对应的数组的总和。

详细的描述为：
<img src="https://t9qtdg.ch.files.1drv.com/y4mf4IVU339kjC2khbuY3ucoknZ_BkOTbLAIKcM6y0AJFVLzWIn5ZNpQm62GcqeeJlkDtx0eA6k8wXP5AS6KBixolvB2kayeqHk1jIFvcDL9SfeA6KeoxCadIbf9qIIkwyMYimDkyHKUpBNv5xY5-D0kiUqS3z1_QuteCZmBAYCg_hCAIhQ7ZV9_d5h8Y2hotm-N4xUVmzoc43fCjeOGB3dZw?width=1716&height=246&cropmode=none" width="1716" height="246" />

其中f 和 c 不需要做过多的描述，需要说明一下，tree的生成。tree的生成主要依靠的是f的下标，例如:
f下标开始是1：tree[1] = f[1],
然后2进制累计：f[1]+f[2] = tree[2],
再然后就是： f[1]+f[2]+f[4] = tree[4]
再然后就是：f[1]+f[2]+f[4]+f[8] = tree[8]
......
最后：f[1]+f[2]+f[4]+f[8]+f[16] = tree[16]

这样的话，tree中还没有填充完毕，对应的源数组的数据也没有使用完，那就是再来一轮，但是这轮中有一个特点就是要求连续，源数组中使用过的元素把这一轮截成一段一顿的，具体的标识为：

f[1],f[2]已经使用过了，从f[3]开始：tree[3]=f[3],接着应该f[3]+f[4],但是f[4] 已经使用过了，那么这段就结束了。

再从f[5] 重新开始，tree[5]=f[5],接着应该f[5]+f[6]= tree[6],在接着应该是f[5]+f[6]+f[8] 也就是 f[4+2的0次方]+f[4+2的1次方]+f[4+2的2次方] 但是f[8] 已经使用过了，就停止该轮。

再次重新开始，从f[7]开始,tree[7]=f[7]，然后f[8] 已经使用过了，就停止该轮。
再次开始，从f[9]开始，tree[9]=f[9]，然后就是f[8+2的0次方]+ f[8+2的1次方] = f[9]+f[10]=tree[10],在接着，就是：f[8+2的0次方]+ f[8+2的1次方] + f[8+2的2次方] = f[9]+f[10]+f[12]=tree[12],,在接着，就是：f[8+2的0次方]+ f[8+2的1次方] + f[8+2的2次方]+f[8+2的3次方] = f[9]+f[10]+f[12]+f[16] 但是f[16] 已经使用过了，停止该轮。

再次开始，从f[11]开始,tree[11]=f[11]，然后f[12] 已经使用过了，就停止该轮。

再次开始，从f[13]开始,tree[13]=f[13]，然后f[14] 已经使用过了，就停止该轮。

再次开始，从f[15]开始,tree[15]=f[15]，然后f[16] 已经使用过了，就停止该轮。
至此，f中的数据全部的使用完毕，tree中的数据也全部的设置完毕，具体的对应关系如下表：

<img src="https://t9ou6w.ch.files.1drv.com/y4mg-aI9zx2Z17S-NYrAhs0c57JJrnNqvN2f5rNVIrkczYS1e9DEk8yZXSeU3SldApIZkwedJMkhiRtRnpxa6rNTO07IpLOj8aS-oVJkAVBggMSjGGCmyH-48tWkBx78ObplenunDl5lFwkuuhbUApt5DqFe69T0c00BAb-_G7Dp-2Bi6Bu1tCHpi5bYrdLQqbjKByumSx7U9D4-5pbs8Rfzg?width=1972&height=140&cropmode=none" width="1972" height="140" />

使用柱状图进行表达：

<img src="https://idhbva.ch.files.1drv.com/y4m7yioge8YIhUwIVfjKT9B3UXBgdNMzCv-K1uxA5EacKIyiXIMgaek_oIwg9iMbyT9Nfio0KWQrgCpAk8joq_8l9B4UVozeW5ktzot7ekU43jWLCKMF6B5FlY92ZlGYc2mdpiNjMCTuPeFNJyyiXHYvQwBYachqk0Xi0nU2aTGQS1vTlR4shzBhFUECUiYOfqAlWqoQeT40HgWooWhKQ5wqw?width=684&height=1244&cropmode=none" width="684" height="1244" />

括号里面为源数组对应的数值，对应的数字为下标值，柱子代表和的相加项。

有了BIT的数组以后，如果我们需要操作2，得到下标为13的和，也就是求取1到13的和。表述为：sum(13),在二级制表达中，13=1101，有趣的是sum(13) = tree(1101)+tree(1100)+tree(1000) 括号内都是二进制表达。我们将在以后更详细地揭示这种联系

#### 隔离最后一位

Note：为了简洁起见，我们将使用“最后一位”来指代相应整数二进制表达的时候，最后一个1. 例如100 的二进制表述为：110 0100，相对应的最后一位为：110 0**1**00

BIT的算法要求数字的最后一位，所以我们需要一种更加高效的方法找到最后一位。例如，n为一个int数字，我们可以使用a1b来表示它的二进制，其中a标识二进制中最后一位1前面的数字，b标识二进制中最后一位1后面的0。 所以int数字n可以表述为：
~~~

n = a1b = a0b+1

//负数的在计算机中，使用补码进行表示 `表示反码的操作
-n = (a1b)` + 1 = a`0b` + 1 // b为全0的序列
   =  a`0(0...0)` +1
   =  a`0(1...1) +1
   =  a`1b
~~~

根据我们的推到，就可以使用JAVA中的&运算取出最后一位了，如下所示：
~~~
       a1b       // 这个就是n
&     a`1b       // 这个就是n的补码表示：反码+1，也就是-n
——————–—————–
= (0…0)1(0…0)

这也即是 n & -n 就能够得到最后一位1的二进制表述了。

~~~

#### 获取累计和：Sum(int j)

为了计算出来sum(idx)对应的值，我们需要以下的步骤：
1. 初始化sum为0
2. sum = sum+ tree[idx]
3. 从自身中减去idx的最后一位（即，将idx的最低有效非零位设置为零）;
并在idx大于零时重复步骤2到3。

使用JAVA代码表述为：

~~~
int read(int idx){
    int sum = 0;
    while (idx > 0){
        sum += tree[idx];
        idx -= (idx & -idx);
    }
    return sum;
}
~~~

以13位例子，操作的数据，如图所示：
<img src="https://idj2ma.ch.files.1drv.com/y4mwOH0dAkjm6T2v7GJd1LJ_BoMB-P7__RcfQq7QTQmBHFZ29rNeW6LYUlCZqZZt3bhfaMT5ox5xIqpbSqoe7g3rt68TwUU3uhwYSTVVns54uc0s1zAP9KSO4-eaDn4WZjkxIUxXVWcJD7wYAdqd728yStrJs_wm55089b1Wk-pcXUCyX2p40_snpV11oVSWDh1l44NqdJiOXGG6b79DM50mA?width=926&height=292&cropmode=none" width="926" height="292" />

所以，我们可以看到，**获取累计和的操作的时间复杂度为O(log MaxIdx),代码的长度也只需要不到10行**

#### 修改源数组某一个值的更新操作：modify(int i,int value)

假设，我们修改了idx对应的值,相应的想法有两种：
1. 根据生成的规则，我们只需要修改idx，idx+2的0次方，idx+2的1次方，..., 直到超出Maidx或者遇到已经使用的idx就停下来。这种方法实现起来比较的复杂。

2. 我们换一个思路，**我们在求解某一个idx的sum值的时候，可以确定这个idx关联的个序列，所以我们在更新的idx值，影响的也是那个序列，只需要更新相关联的那个序列即可。**


对应的代码如下：
~~~
void update(int idx, int val){
    while (idx <= MaxIdx){
        tree[idx] += val;
        idx += (idx & -idx);
    }
}
~~~

然后我们很容易得到，修改的时间复杂度为：O(log MaxIdx) 实现起来也非常的简单。

#### 获得某一个位置的确定值

如果我们想获得源数组的某一个位置的值，我们不能直接的返回tree(idx).

一种的方法，就是我们保留着源数组，在生成tree数组的时候，从新的申请空间，这样的话，获得某一个位置的确定值就是O(1), 当然时间复杂度也就是线性的。

第二种方法，是不申请额外的空间，我们可以使用 read[idx]-read[idx-1]=f[idx] ,通过两次的获取操作（idx和idx-1）来计算f[idx] ，时间复杂度为2*log(MaxIdx).

第三种种方法是，运行时间复杂度比调用读取两次（降低一个恒定因子）要低。
该方法背后的主要思想是基于以下观察:假设我们要计算两个索引之间的总和,sum(i,j), 对于两个索引中的每一个，请考虑从索引到根的路径。 这两个路径在某个索引处相遇（最晚在索引0处），此后它们重叠。 然后，我们可以计算沿这两个路径中的每个路径的相加之和，直到它们相遇并减去这两个和。 这样，我们就可以得出两个索引之间的频率之和。

sum[i] = tree[i]+tree[m]+tree[n]+...+tree[0]
sum[j] = tree[j]+tree[k]+tree[p]+tree[n]...+tree[0]

我们将此观察结果转换为以下算法。

令x为索引，y = x-1。 
我们可以将y表示为a0b（用二进制表示），其中b包含所有1。 
那么，x是a1b（注意b由所有零组成）。 
现在，考虑应用于x的算法的第一次迭代。 
在第一次迭代中，该算法删除了x的最后一位，因此将x替换为z = a0b。

现在，让我们考虑函数读取的活动索引idx如何在输入y的每次迭代之间变化。
读取的函数将idx的最后一位一一删除。
经过几个步骤，活动索引idx变为a0b（提醒一下，最初idx等于y = a0b），与z相同。在这一点上，我们停止了两条路径相遇，一条路径来自x，另一条路径来自y。现在，我们可以编写类似于此讨论的算法。

~~~
int readSingle(int idx) {
	int sum = tree[idx]; // this sum will be decreased
	if (idx > 0) { // the special case
		int z = idx - (idx & -idx);
		idx--; // idx is not important anymore, so instead y, you can use idx
		while (idx != z) { // at some iteration idx (y) will become z
			sum -= tree[idx];
			// substruct tree frequency which is between y and "the same path"
			idx -= (idx & -idx);
		}
	}
	return sum;
}
~~~



