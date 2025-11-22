digraph arg {
label="";
node_0 [label="{0=XcfaProcessState{main_init {init} initialized=false}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_1 [label="{0=XcfaProcessState{main_init {init} initialized=true}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_2 [label="{0=XcfaProcessState{__loc_48  initialized=true}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_3 [label="{0=XcfaProcessState{__loc_92  initialized=true}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_5 [label="{0=XcfaProcessState{main_error {error} initialized=true}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",peripheries=2,style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_6 [label="{0=XcfaProcessState{main_final {final} initialized=true}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_4 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
phantom_init0 [label="\n",style="solid,filled",fillcolor="#FFFFFF",color="#FFFFFF",shape=ellipse];
node_0 -> node_1 [label="0: main_init {init} -> main_init {init} [[skip]]\l",color="#000000",style=solid,fontname="courier"];
node_1 -> node_2 [label="0: main_init {init} -> __loc_48  [[(assign main::a 6), (assign main::b 6), (assign main::a 0)]]\l",color="#000000",style=solid,fontname="courier"];
node_2 -> node_3 [label="0: __loc_48  -> __loc_92  [[(assume (not (< main::a 6)))]]\l",color="#000000",style=solid,fontname="courier"];
node_2 -> node_4 [label="0: __loc_48  -> __loc_65  [[(assume (< main::a 6)), (assign main::b 0)]]\l",color="#000000",style=solid,fontname="courier"];
node_3 -> node_5 [label="0: __loc_92  -> main_error {error} [[(assume (not (and (/= 0 (ite (= main::a 6) 1 0)) (/= 0 (ite (= main::b 6) 1 0)))))]]\l",color="#000000",style=solid,fontname="courier"];
node_3 -> node_6 [label="0: __loc_92  -> main_final {final} [[(assume (and (/= 0 (ite (= main::a 6) 1 0)) (/= 0 (ite (= main::b 6) 1 0)))), (assign main_ret 1)]]\l",color="#000000",style=solid,fontname="courier"];
phantom_init0 -> node_0 [label="\n",color="#000000",style=solid];
}


digraph arg {
label="";
node_0 [label="{0=XcfaProcessState{main_init {init} initialized=false}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_1 [label="{0=XcfaProcessState{main_init {init} initialized=true}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_7 [label="{0=XcfaProcessState{__loc_48  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_8 [label="{0=XcfaProcessState{__loc_92  initialized=true}} {PtrState(innerState=(PredState false), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_9 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_11 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_10 [label="{0=XcfaProcessState{__loc_48  initialized=true}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_13 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_12 [label="{0=XcfaProcessState{__loc_92  initialized=true}} {PtrState(innerState=(PredState (>= main::a 6)), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_15 [label="{0=XcfaProcessState{main_final {final} initialized=true}} {PtrState(innerState=(PredState (>= main::a 6)), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_14 [label="{0=XcfaProcessState{main_error {error} initialized=true}} {PtrState(innerState=(PredState (>= main::a 6)), nextCnt=0), mutex={0=0}}\l",peripheries=2,style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
phantom_init0 [label="\n",style="solid,filled",fillcolor="#FFFFFF",color="#FFFFFF",shape=ellipse];
node_0 -> node_1 [label="0: main_init {init} -> main_init {init} [[skip]]\l",color="#000000",style=solid,fontname="courier"];
node_1 -> node_7 [label="0: main_init {init} -> __loc_48  [[(assign main::a 6), (assign main::b 6), (assign main::a 0)]]\l",color="#000000",style=solid,fontname="courier"];
node_7 -> node_8 [label="0: __loc_48  -> __loc_92  [[(assume (not (< main::a 6)))]]\l",color="#000000",style=solid,fontname="courier"];
node_7 -> node_9 [label="0: __loc_48  -> __loc_65  [[(assume (< main::a 6)), (assign main::b 0)]]\l",color="#000000",style=solid,fontname="courier"];
node_9 -> node_11 [label="0: __loc_65  -> __loc_65  [[(assume (< main::b 6)), (assign main::b (+ main::b 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_9 -> node_10 [label="0: __loc_65  -> __loc_48  [[(assume (not (< main::b 6))), (assign main::a (+ main::a 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_10 -> node_13 [label="0: __loc_48  -> __loc_65  [[(assume (< main::a 6)), (assign main::b 0)]]\l",color="#000000",style=solid,fontname="courier"];
node_10 -> node_12 [label="0: __loc_48  -> __loc_92  [[(assume (not (< main::a 6)))]]\l",color="#000000",style=solid,fontname="courier"];
node_12 -> node_15 [label="0: __loc_92  -> main_final {final} [[(assume (and (/= 0 (ite (= main::a 6) 1 0)) (/= 0 (ite (= main::b 6) 1 0)))), (assign main_ret 1)]]\l",color="#000000",style=solid,fontname="courier"];
node_12 -> node_14 [label="0: __loc_92  -> main_error {error} [[(assume (not (and (/= 0 (ite (= main::a 6) 1 0)) (/= 0 (ite (= main::b 6) 1 0)))))]]\l",color="#000000",style=solid,fontname="courier"];
phantom_init0 -> node_0 [label="\n",color="#000000",style=solid];
}

digraph G {
 label="";
 subgraph cluster_0 {
 label="reach_error";
 reach_error_init[];
 reach_error_final[];
 reach_error_init -> reach_error_final [label="SequenceLabel[] "];
 
 }
 subgraph cluster_1 {
 label="__VERIFIER_assert";
 __VERIFIER_assert_init[];
 __VERIFIER_assert_final[];
 __loc_14[];
 __VERIFIER_assert_error[];
 __VERIFIER_assert_init -> __loc_14 [label="SequenceLabel[] "];
 __loc_14 -> __VERIFIER_assert_error [label="SequenceLabel[((assume (= __VERIFIER_assert::cond 0)))[choiceType=MAIN_PATH]] "];
 __loc_14 -> __VERIFIER_assert_final [label="SequenceLabel[((assume (not (= __VERIFIER_assert::cond 0))))[choiceType=ALTERNATIVE_PATH],\n(assign __VERIFIER_assert_ret 0)] "];
 
 }
 subgraph cluster_2 {
 label="main";
 main_init[];
 main_final[];
 __loc_55[];
 __loc_1486[];
 main_error[];
 partial0[];
 partial1[];
 partial2[];
 __loc_1486 -> main_error [label="SequenceLabel[((assume (= __VERIFIER_assert::cond 0)))[choiceType=MAIN_PATH]] "];
 __loc_55 -> __loc_55 [label="SequenceLabel[((assume (<= i n)))[choiceType=MAIN_PATH],\n(assign sn (+ sn 2)),\n(assign i (+ i 1))] "];
 __loc_1486 -> partial0 [label="SequenceLabel[((assume (not (= __VERIFIER_assert::cond 0))))[choiceType=ALTERNATIVE_PATH],\n(assign __VERIFIER_assert_ret 0)] "];
 partial0 -> main_final [label="SequenceLabel[(assume (or (and (or (= (+ (* 2 n) (* -1 sn)) 0) (= sn 0)) (not (= 0 __VERIFIER_assert::cond)) (>= sn 0) (iff (>= (+ n (* -1 i)) 0) (>= n 1)) (<= sn 0) (<= i 1) (<= -2 (+ (* -2 i) sn))) (and (or (= (+ (* 2 n) (* -1 sn)) 0) (= sn 0)) (not (= 0 __VERIFIER_assert::cond)) (>= sn 0) (not (iff (>= (+ n (* -1 i)) 0) (>= n 1))) (not (<= sn 0)) (not (<= i 1)) (or (= (+ (* 2 n) (* -1 sn)) 0) (not (<= (+ (* 2 n) (* -1 sn)) 0))) (<= -2 (+ (* -2 i) sn))) (and (not (= 0 __VERIFIER_assert::cond)) (>= sn 0) (not (<= sn 0)) (<= -2 (+ (* -2 i) sn)))))] "];
 main_init -> partial1 [label="SequenceLabel[(havoc n),\n(assume (and (>= n -2147483648) (<= n 2147483647))),\n(assign sn 0),\n(assign i 1)] "];
 partial1 -> __loc_55 [label="SequenceLabel[(assume (or (and (or (= (+ (* 2 n) (* -1 sn)) 0) (= sn 0)) (>= sn 0) (iff (>= (+ n (* -1 i)) 0) (>= n 1)) (<= sn 0) (<= i 1) (<= -2 (+ (* -2 i) sn))) (and (>= sn 0) (not (<= sn 0)) (or (= (+ (* 2 n) (* -1 sn)) 0) (not (<= (+ (* 2 n) (* -1 sn)) 0))) (<= -2 (+ (* -2 i) sn))) (and (>= sn 0) (not (<= sn 0)) (<= -2 (+ (* -2 i) sn))) (and (>= sn 0) (not (<= sn 0)) (<= -2 (+ (* -2 i) sn)))))] "];
 __loc_55 -> partial2 [label="SequenceLabel[((assume (not (<= i n))))[choiceType=ALTERNATIVE_PATH],\n(assign __VERIFIER_assert::cond (+ (ite (or (/= 0 (ite (= (+ sn) (+ (+ (* (+ n) (+ 2))))) 1 0)) (/= 0 (ite (= (+ sn) (+ 0)) 1 0))) 1 0)))] "];
 partial2 -> __loc_1486 [label="SequenceLabel[(assume (or (and (or (= (+ (* 2 n) (* -1 sn)) 0) (= sn 0)) (not (= 0 __VERIFIER_assert::cond)) (>= sn 0) (iff (>= (+ n (* -1 i)) 0) (>= n 1)) (<= sn 0) (<= i 1) (<= -2 (+ (* -2 i) sn))) (and (or (= (+ (* 2 n) (* -1 sn)) 0) (= sn 0)) (not (= 0 __VERIFIER_assert::cond)) (>= sn 0) (not (iff (>= (+ n (* -1 i)) 0) (>= n 1))) (not (<= sn 0)) (not (<= i 1)) (or (= (+ (* 2 n) (* -1 sn)) 0) (not (<= (+ (* 2 n) (* -1 sn)) 0))) (<= -2 (+ (* -2 i) sn))) (and (>= sn 0) (not (<= sn 0)) (<= -2 (+ (* -2 i) sn)))))] "];
 
 }
 }

(2n=sn) or(sn=0)
(ns>=0) 
(<= main::sn 0) 
(<= main::i 1) 
(or (= (+ (* 2 main::n) (* -1 main::sn)) 0) (not (<= (+ (* 2 main::n) (* -1 m
ain::sn)) 0))) 
sn = 2(i-1)
i>=1 
(or (<= (+ (* 2 main::n) (* -1 main::sn)) 0) (>= (+ main::n (* -1 main::i)) 0))) 

digraph arg {
label="";
node_0 [label="{0=XcfaProcessState{main_init {init} initialized=false}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_1 [label="{0=XcfaProcessState{main_init {init} initialized=true}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_7 [label="{0=XcfaProcessState{__loc_48  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_8 [label="{0=XcfaProcessState{__loc_92  initialized=true}} {PtrState(innerState=(PredState false), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_16 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6)) (not (>= main::b 6))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_18 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_19 [label="{0=XcfaProcessState{__loc_48  initialized=true}} {PtrState(innerState=(PredState (>= main::b 6)), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_22 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6)) (not (>= main::b 6))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_21 [label="{0=XcfaProcessState{__loc_92  initialized=true}} {PtrState(innerState=(PredState (>= main::a 6) (>= main::b 6)), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_23 [label="{0=XcfaProcessState{main_error {error} initialized=true}} {PtrState(innerState=(PredState (>= main::a 6) (>= main::b 6)), nextCnt=0), mutex={0=0}}\l",peripheries=2,style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_24 [label="{0=XcfaProcessState{main_final {final} initialized=true}} {PtrState(innerState=(PredState (>= main::a 6) (>= main::b 6)), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_20 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_17 [label="{0=XcfaProcessState{__loc_48  initialized=true}} {PtrState(innerState=(PredState false), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
phantom_init0 [label="\n",style="solid,filled",fillcolor="#FFFFFF",color="#FFFFFF",shape=ellipse];
node_0 -> node_1 [label="0: main_init {init} -> main_init {init} [[skip]]\l",color="#000000",style=solid,fontname="courier"];
node_1 -> node_7 [label="0: main_init {init} -> __loc_48  [[(assign main::a 6), (assign main::b 6), (assign main::a 0)]]\l",color="#000000",style=solid,fontname="courier"];
node_7 -> node_8 [label="0: __loc_48  -> __loc_92  [[(assume (not (< main::a 6)))]]\l",color="#000000",style=solid,fontname="courier"];
node_7 -> node_16 [label="0: __loc_48  -> __loc_65  [[(assume (< main::a 6)), (assign main::b 0)]]\l",color="#000000",style=solid,fontname="courier"];
node_16 -> node_18 [label="0: __loc_65  -> __loc_65  [[(assume (< main::b 6)), (assign main::b (+ main::b 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_16 -> node_17 [label="0: __loc_65  -> __loc_48  [[(assume (not (< main::b 6))), (assign main::a (+ main::a 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_18 -> node_19 [label="0: __loc_65  -> __loc_48  [[(assume (not (< main::b 6))), (assign main::a (+ main::a 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_18 -> node_20 [label="0: __loc_65  -> __loc_65  [[(assume (< main::b 6)), (assign main::b (+ main::b 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_19 -> node_22 [label="0: __loc_48  -> __loc_65  [[(assume (< main::a 6)), (assign main::b 0)]]\l",color="#000000",style=solid,fontname="courier"];
node_19 -> node_21 [label="0: __loc_48  -> __loc_92  [[(assume (not (< main::a 6)))]]\l",color="#000000",style=solid,fontname="courier"];
node_21 -> node_23 [label="0: __loc_92  -> main_error {error} [[(assume (not (and (/= 0 (ite (= main::a 6) 1 0)) (/= 0 (ite (= main::b 6) 1 0)))))]]\l",color="#000000",style=solid,fontname="courier"];
node_21 -> node_24 [label="0: __loc_92  -> main_final {final} [[(assume (and (/= 0 (ite (= main::a 6) 1 0)) (/= 0 (ite (= main::b 6) 1 0)))), (assign main_ret 1)]]\l",color="#000000",style=solid,fontname="courier"];
node_17 -> node_7 [label="\n",color="#000000",style=dashed,weight="0"];
phantom_init0 -> node_0 [label="\n",color="#000000",style=solid];
}

digraph arg {
label="";
node_0 [label="{0=XcfaProcessState{main_init {init} initialized=false}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_1 [label="{0=XcfaProcessState{main_init {init} initialized=true}} {PtrState(innerState=(PredState), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_7 [label="{0=XcfaProcessState{__loc_48  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_25 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6))\l           (not (>= main::b 6))\l           (<= main::b 0)\l           (<= main::b 1)), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_26 [label="{0=XcfaProcessState{__loc_48  initialized=true}} {PtrState(innerState=(PredState false), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_27 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6)) (not (>= main::b 6)) (<= main::b 1)), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_28 [label="{0=XcfaProcessState{__loc_48  initialized=true}} {PtrState(innerState=(PredState false), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_29 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6)) (not (>= main::b 6))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_31 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_33 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_32 [label="{0=XcfaProcessState{__loc_48  initialized=true}} {PtrState(innerState=(PredState (>= main::b 6) (not (<= main::b 0)) (not (<= main::b 1))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_34 [label="{0=XcfaProcessState{__loc_92  initialized=true}} {PtrState(innerState=(PredState (>= main::a 6)\l           (>= main::b 6)\l           (not (<= main::b 0))\l           (not (<= main::b 1))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_37 [label="{0=XcfaProcessState{main_final {final} initialized=true}} {PtrState(innerState=(PredState (>= main::a 6)\l           (>= main::b 6)\l           (not (<= main::b 0))\l           (not (<= main::b 1))), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_36 [label="{0=XcfaProcessState{main_error {error} initialized=true}} {PtrState(innerState=(PredState (>= main::a 6)\l           (>= main::b 6)\l           (not (<= main::b 0))\l           (not (<= main::b 1))), nextCnt=0), mutex={0=0}}\l",peripheries=2,style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_35 [label="{0=XcfaProcessState{__loc_65  initialized=true}} {PtrState(innerState=(PredState (not (>= main::a 6))\l           (not (>= main::b 6))\l           (<= main::b 0)\l           (<= main::b 1)), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_30 [label="{0=XcfaProcessState{__loc_48  initialized=true}} {PtrState(innerState=(PredState false), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
node_8 [label="{0=XcfaProcessState{__loc_92  initialized=true}} {PtrState(innerState=(PredState false), nextCnt=0), mutex={0=0}}\l",style="solid,filled",fontname="courier",fillcolor="#FFFFFF",color="#000000",shape=rectangle];
phantom_init0 [label="\n",style="solid,filled",fillcolor="#FFFFFF",color="#FFFFFF",shape=ellipse];
node_0 -> node_1 [label="0: main_init {init} -> main_init {init} [[skip]]\l",color="#000000",style=solid,fontname="courier"];
node_1 -> node_7 [label="0: main_init {init} -> __loc_48  [[(assign main::a 6), (assign main::b 6), (assign main::a 0)]]\l",color="#000000",style=solid,fontname="courier"];
node_7 -> node_25 [label="0: __loc_48  -> __loc_65  [[(assume (< main::a 6)), (assign main::b 0)]]\l",color="#000000",style=solid,fontname="courier"];
node_7 -> node_8 [label="0: __loc_48  -> __loc_92  [[(assume (not (< main::a 6)))]]\l",color="#000000",style=solid,fontname="courier"];
node_25 -> node_26 [label="0: __loc_65  -> __loc_48  [[(assume (not (< main::b 6))), (assign main::a (+ main::a 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_25 -> node_27 [label="0: __loc_65  -> __loc_65  [[(assume (< main::b 6)), (assign main::b (+ main::b 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_26 -> node_7 [label="\n",color="#000000",style=dashed,weight="0"];
node_27 -> node_28 [label="0: __loc_65  -> __loc_48  [[(assume (not (< main::b 6))), (assign main::a (+ main::a 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_27 -> node_29 [label="0: __loc_65  -> __loc_65  [[(assume (< main::b 6)), (assign main::b (+ main::b 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_28 -> node_7 [label="\n",color="#000000",style=dashed,weight="0"];
node_29 -> node_31 [label="0: __loc_65  -> __loc_65  [[(assume (< main::b 6)), (assign main::b (+ main::b 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_29 -> node_30 [label="0: __loc_65  -> __loc_48  [[(assume (not (< main::b 6))), (assign main::a (+ main::a 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_31 -> node_33 [label="0: __loc_65  -> __loc_65  [[(assume (< main::b 6)), (assign main::b (+ main::b 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_31 -> node_32 [label="0: __loc_65  -> __loc_48  [[(assume (not (< main::b 6))), (assign main::a (+ main::a 1))]]\l",color="#000000",style=solid,fontname="courier"];
node_32 -> node_34 [label="0: __loc_48  -> __loc_92  [[(assume (not (< main::a 6)))]]\l",color="#000000",style=solid,fontname="courier"];
node_32 -> node_35 [label="0: __loc_48  -> __loc_65  [[(assume (< main::a 6)), (assign main::b 0)]]\l",color="#000000",style=solid,fontname="courier"];
node_34 -> node_37 [label="0: __loc_92  -> main_final {final} [[(assume (and (/= 0 (ite (= main::a 6) 1 0)) (/= 0 (ite (= main::b 6) 1 0)))), (assign main_ret 1)]]\l",color="#000000",style=solid,fontname="courier"];
node_34 -> node_36 [label="0: __loc_92  -> main_error {error} [[(assume (not (and (/= 0 (ite (= main::a 6) 1 0)) (/= 0 (ite (= main::b 6) 1 0)))))]]\l",color="#000000",style=solid,fontname="courier"];
node_30 -> node_7 [label="\n",color="#000000",style=dashed,weight="0"];
phantom_init0 -> node_0 [label="\n",color="#000000",style=solid];
}

assume 6 < a  or false or  6 >= b 
assume a > 6 or  b >= 6

(a < 6) or ( b >= 6)
a < 6    b=whatever 
a = whatever b>=6
not (a ≥ 6 and b < 6)
