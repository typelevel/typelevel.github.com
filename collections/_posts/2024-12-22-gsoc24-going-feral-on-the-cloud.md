---
layout: post
title: Google Summer of Code 2024 - Going Feral on The Cloud
category: technical

meta: 
  nav: blog
  author: chingles
---

This project was proposed by the Typelevel community in collaboration with the Scala Center, and carried out under Google Summer of Code (GSoC) 2024. Feral is a library in the Typelevel ecosystem that provides a framework for Scala developers to write and deploy serverless functions. As Feral was only supporting AWS Lambda, the goal of the project was to extend Feral to support other serverless providers, specifically Vercel and Google Cloud.

The vision for Feral is to enable Scala developers to easily switch between one cloud provider to another that better suits their needs, without the need for major refactoring of their codebases. Such convenience would give developers greater freedom as they do not need to be tied down to one platform. Furthermore, as Feral is part of the Typelevel ecosystem, developers who are currently using the Http4s Typelevel library for non-serverless web services may also effortlessly make the switch to serverless.

With these goals in mind, it is imperative to provide robust support for the common serverless providers, which is what the project aimed to work towards.

## What I Did

| Pull Request (PR) | Status | Comments |
| --- | --- | --- |
| [Add `ApiGatewayV2WebSocketEvent`](https://github.com/typelevel/feral/pull/476) | Merged | The addition of this AWS Lambda event provided an introduction to serverless functions for me, while enhancing the support that Feral has for AWS Lambda. |
| [Created support for Vercel using Node.js runtime](https://github.com/typelevel/feral/pull/492) | Not Merged | Vercel's implementation of routes conflicts with the way Http4s handles routes. Due to this incompatibility between Feral and Vercel, this PR was not merged. Through working on this, there is a better understanding of how Vercel and Http4s work, which could pave the way for future work. |
| [Created support for Google Cloud HTTP functions](https://github.com/typelevel/feral/pull/498) | Merged | This PR enabled support for both JVM and Node.js runtimes. There is a minor error logged in the JVM runtime implementation that we minimized to a bug unrelated to Feral or any of the Typelevel libraries. The error does not seem to impact the functionality of the resulting web application, but it would be good to further investigate the cause of it. |

## Challenges and Lessons Learnt
It was challenging to support various serverless platforms, particularly Vercel and Google Cloud, which I created the initial implementation for. While I learnt along the way that the general procedure for supporting each platform was generally the same, where one would have to do things such as converting between types and using a dispatcher, there were still differences in certain details. For example, while referencing the pre-existing Feral implementation to support AWS Lambda event functions in order to support Google Cloud HTTP functions for the JVM runtime, I learnt that HTTP functions are a subset of AWS Lambda event functions, while they were separate in Google Cloud. 

My lack of familiarity with Scala and functional programming also posed a hindrance. While I had previously done some functional programming with Scala prior to GSoC, it was still something rather new to me. As such, it took me a longer time to write code and debug than the time I would probably have taken if I was more familiar. However, this GSoC project gave me the opportunity to improve myself in these areas. I became more familiar with previously-learnt concepts such as monads, and learnt new things such as for-yield statements and how they can be desugared. 

Through GSoC, I have learnt many new things while reinforcing what I already know. I am grateful that the Typelevel organization held a session that taught the concept of programs-as-values as it was something that I never knew existed. I also learnt to appreciate what I have learnt in school better, by experiencing first-hand how I can apply such knowledge in real-world situations. For example, this project utilized the concept of resource allocation which was something I had previously learnt about.

## Future Work
I plan to continue contributing to enhancing Feral after GSoC 2024 ends. Some ways I could do this is to create support for Google Cloud Event functions and create SBT plug-ins to test Google Cloud functions locally as well as deploy the functions. If possible, investigation could also be done on how, if possible, Vercel can be integrated into Feral without impeding developers from using Http4s with it. 

## Acknowledgements
I would first like to thank my mentors, [Arman](https://github.com/armanbilge) and [Antonio](https://github.com/toniogela) for their constant guidance during GSoC. In particular, I would like to thank Arman for taking the time to set up weekly pair programming sessions, which has enhanced my learning experience greatly. I would also like to thank the Scala Center and the Typelevel community for proposing and supporting this project. Lastly, I would like to thank Google for hosting GSoC 2024 and providing me with the opportunity to learn about open source projects.
