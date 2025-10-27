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

