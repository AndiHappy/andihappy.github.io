---
layout: default
title: 人生周报
description: 个人知识管理，随手笔记更新备忘。
keywords: 人生周报
permalink: /wiki/
---

<ul>
{% for wiki in site.wiki %}
{% if wiki.title != "Wiki Template" %}
<li><a href="{{ site.url }}{{ wiki.url }}">{{ wiki.title }}</a></li>
{% endif %}
{% endfor %}
</ul>
