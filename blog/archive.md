---
layout: page
title: "Blog Archive"

meta:
  nav: blog
---

<ul>
	{% for page in site.posts %}
		<li>
			<a href="{{ page.url }}">{{ page.title }}</a>
			({{ page.date | date: "%B %Y" }})
		</li>
	{% endfor %}
</ul>
