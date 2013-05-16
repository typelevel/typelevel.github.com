---
layout: page
title: Blog

meta:
  nav: blog
  canonical: blog
---

Blog
====

<ul class="post-list">
{% for page in site.posts %}
  <li>
    <a href="{{ site.baseurl }}{{ page.url }}">{{ page.title }}</a>
    {% if page.meta.author %}
      by {% include author.html %}
    {% endif %}
    ({{ page.date | date: "%B %Y" }})
  </li>
{% endfor %}
</ul>
