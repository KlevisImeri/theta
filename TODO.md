- Add explicit state example 
    - check if it gives good results if yes then the size of the location invariants of the explisit state is a problem

- Simplification can be slow
- Maybe wen you do the locationinvarit Pass added a new node is worse
- the place of simplication is weird
- Possible that simplifcaiont does not have nay benifit


- Write different XcfaConfigs and make protofiloes as diff as possible1
Some idea why things are not working:
I am having location invariants that by the time the next config that specific location it has caculated thos invariants so it is not adding any value
=>  I think the best bet is combining as different backends as possible
I as using PredBoolDefault->PredBoolConjuncts"
and ExplDefault->ExplFull
they probably produce very similar paths and invariants so the invariants i add dont add a lot fo value
i Will make some configs and try so many different combination at the svbenchcloud thing

- Try clever ways to unroll


DO one round of ExplState with good init precision -> pred state
