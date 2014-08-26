## usage

* sbt run

```
(in sys1) resolve(akka://sys1/user/n1) -> ActorSelection[Anchor(akka://sys1/), Path(/user/n1)]
(in sys1) resolve(akka://sys2/user/n1) -> ActorSelection[Anchor(akka://sys1/deadLetters), Path(/user/n1)]
(in sys2) resolve(akka://sys1/user/n1) -> ActorSelection[Anchor(akka://sys2/deadLetters), Path(/user/n1)]
(in sys2) resolve(akka://sys2/user/n1) -> ActorSelection[Anchor(akka://sys2/), Path(/user/n1)]
```
