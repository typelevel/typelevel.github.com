---
layout: event

title: "Typelevel Community Conference Cádiz"
short_title: "Cádiz Community Conference"
date_string: "September 30, 2016"
location: "Palacio de Congresos de Cádiz"
description: "Co-located with Lambda World"

poster_hero: "/img/media/cadiz.jpg"
poster_thumb: "/img/media/cadiz-thumb.jpg"

location_section: true
sponsors_section: true

schedule:

  - time: "--:--"
    title: "Free like the wind and the summer"
    speakers: ["dialelo"]
    summary: "Free Monads and Applicatives are being used by functional programmers for writing applications by separating a DSL for the problem domain from the interpreter for such DSL. This allows us to write our application logic in a purely functional way and do the IO and other effectful computations in the interpreters, using a effect-capturing monad when running our programs. Although they bring great benefits in modularity and composability they have their tradeoffs in terms of boilerplate, performance and simplicity. In this talk I'll explain the strengths and weaknesses of Free Monads/Applicatives for writing both applications and libraries, explore the available libraries in Scala for using this technique and provide examples from our real-world uses of Free. I'll also speak about some of the gotchas that you may find when using Free, provide some tips and suggest future work that can benefit everyone using Free structures."
    
sponsors:
  - name: "47 Degrees"
    logo: "/img/media/sponsors/47_degrees.png"
    link: "http://www.47deg.com/"
    type: "platinum"
    height: 100

---

## About the Conference

A day of Typelevel sessions, co-located with [Lambda World](http://www.lambda.world).

On Friday, September 30th,[Lambda World](http://www.lambda.world) will host a Typelevel Unconference during the morning and a few workshops in the afternoon. The day will conclude with a community dinner where attendees can discuss functional concepts while enjoying Flamenco music.

If you're interested in speaking during the unconference, please send us your ideas. This is just to see what people are interested in. There is no review, the actual sessions are decided at the conference! We will publish all the submitted proposals here.

<a class="typeform-share btn large" href="https://jorgegalindocruces.typeform.com/to/dTlYCv" data-mode="2" target="_blank">Send your Proposal</a>
<script>(function(){var qs,js,q,s,d=document,gi=d.getElementById,ce=d.createElement,gt=d.getElementsByTagName,id='typef_orm',b='https://s3-eu-west-1.amazonaws.com/share.typeform.com/';if(!gi.call(d,id)){js=ce.call(d,'script');js.id=id;js.src=b+'share.js';q=gt.call(d,'script')[0];q.parentNode.insertBefore(js,q)}id=id+'_';if(!gi.call(d,id)){qs=ce.call(d,'link');qs.rel='stylesheet';qs.id=id;qs.href=b+'share-button.css';s=gt.call(d,'head')[0];s.appendChild(qs,s)}})()</script>


## Talks proposals

{% assign schedule=page.schedule %}
{% include schedule.html %}

## Venue

This event will take place at the Palacio de Congresos de Cádiz.

{% include venue_map.html %}

## Sponsors

We'd like to thank all our sponsors who help to make the Summit happen:

{% include sponsors.html %}
