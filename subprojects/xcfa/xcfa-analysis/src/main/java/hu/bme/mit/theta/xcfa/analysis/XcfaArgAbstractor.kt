/*
 *  Copyright 2025 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.xcfa.analysis

import hu.bme.mit.theta.analysis.algorithm.abortIfTimedOut;
import com.google.common.base.Preconditions
import hu.bme.mit.theta.analysis.Action
import hu.bme.mit.theta.analysis.Prec
import hu.bme.mit.theta.analysis.State
import hu.bme.mit.theta.analysis.algorithm.arg.ARG
import hu.bme.mit.theta.analysis.algorithm.arg.ArgBuilder
import hu.bme.mit.theta.analysis.algorithm.arg.ArgNode
import hu.bme.mit.theta.analysis.algorithm.cegar.AbstractorResult
import hu.bme.mit.theta.analysis.algorithm.cegar.BasicArgAbstractor
import hu.bme.mit.theta.analysis.algorithm.cegar.abstractor.StopCriterion
import hu.bme.mit.theta.analysis.reachedset.Partition
import hu.bme.mit.theta.analysis.waitlist.Waitlist
import hu.bme.mit.theta.common.logging.Logger
import java.util.function.Function
import hu.bme.mit.theta.analysis.utils.ArgVisualizer;
import hu.bme.mit.theta.common.visualization.writer.GraphvizWriter;
import hu.bme.mit.theta.ui.DEBUG.debug

class XcfaArgAbstractor<S : State, A : Action, P : Prec>(
  argBuilder: ArgBuilder<S, A, P>,
  projection: Function<in S?, *>?,
  waitlist: Waitlist<ArgNode<S, A>>,
  stopCriterion: StopCriterion<S, A>,
  logger: Logger,
) : BasicArgAbstractor<S, A, P>(argBuilder, projection, waitlist, stopCriterion, logger) {

  override fun check(arg: ARG<S, A>, prec: P): AbstractorResult {
    return check(arg, prec, {})
  }

  override fun check(arg: ARG<S, A>, prec: P, inject: () -> Unit): AbstractorResult {
    // logger.write(Logger.Level.INFO, "|  |  Precision: %s%n", prec)
    logger.write(Logger.Level.INFO, "|  |  PrecisionSize: %d%n", prec.getSize())

    if (!arg.isInitialized) {
      logger.write(Logger.Level.SUBSTEP, "|  |  (Re)initializing ARG...")
      argBuilder.init(arg, prec)
      logger.write(Logger.Level.SUBSTEP, "done%n")
    }

    assert(arg.isInitialized)

    logger.write(
      Logger.Level.INFO,
      "|  |  Starting ARG: %d nodes, %d incomplete, %d unsafe%n",
      arg.nodes.count(),
      arg.incompleteNodes.count(),
      arg.unsafeNodes.count(),
    )
    logger.write(Logger.Level.SUBSTEP, "|  |  ${stopCriterion}...%n")
    logger.write(Logger.Level.SUBSTEP, "|  |  Building ARG...%n")

    val reachedSet: Partition<ArgNode<S, A>, *> =
      Partition.of { n: ArgNode<S, A> -> projection.apply(n.state) }
    waitlist.clear()

    reachedSet.addAll(arg.nodes)
    waitlist.addAll(arg.incompleteNodes)

    if (!stopCriterion.canStop(arg)) {  // FIX: very time consuming
      var expansionCounter = 0
      var expansionCounterLimit = 5;
      var injectCnt = 1;

      while (!waitlist.isEmpty) {
        val node = waitlist.remove()
        var newNodes: Collection<ArgNode<S, A>>? = emptyList()
        if ((node.state as XcfaState<*>).xcfa!!.isInlined) {
          close(node, reachedSet[node])
        } else {
          val expandProcedureCall = (node.state as XcfaState<*>) in (prec as XcfaPrec<P>).noPop
          closePop(node, reachedSet[node], !expandProcedureCall)
        }
        if (!node.isSubsumed && !node.isTarget) {
          newNodes = argBuilder.expand(node, prec)
          
          // println(prec);
          // DEBUG.type(arg);
          // val g = ArgVisualizer.getDefault().visualize(arg)
          // println(GraphvizWriter.getInstance().writeString(g))

          reachedSet.addAll(newNodes)
          waitlist.addAll(newNodes)
          
          if(injectCnt==20){
            inject();
            injectCnt = 1;
          };
          injectCnt++;
          
          // if(expansionCounter == expansionCounterLimit) {
          //   logger.write(
          //     Logger.Level.INFO,
          //     "|  |  Expanded: %d new, ARG now %d nodes, %d incomplete%n",
          //     newNodes.size,
          //     arg.nodes.count(),
          //     arg.incompleteNodes.count()
          //   )
          //   expansionCounterLimit*=2;
          //   expansionCounter=0;
          // }
          // expansionCounter++
        }
        if (stopCriterion.canStop(arg, newNodes)) break
      }
    }

    // logger.write(Logger.Level.SUBSTEP, "| Done%n")
    logger.write(
      Logger.Level.INFO,
      "|  |  Finished ARG: %d nodes, %d incomplete, %d unsafe%n",
      arg.nodes.count(),
      arg.incompleteNodes.count(),
      arg.unsafeNodes.count(),
    )

    waitlist.clear() // Optimization

    // val g = ArgVisualizer.getDefault().visualize(arg)
    // println(GraphvizWriter.getInstance().writeString(g))


    return if (arg.isSafe) {
      Preconditions.checkState(arg.isComplete, "Returning incomplete ARG as safe")
      AbstractorResult.safe()
    } else {
      AbstractorResult.unsafe()
    }
  }

  fun closePop(node: ArgNode<S, A>, candidates: Collection<ArgNode<S, A>>, popCovered: Boolean) {
    if (!node.isLeaf) {
      return
    }
    for (candidate in candidates) {
      if (candidate.mayCover(node)) {
        var onlyStackCovers = false
        (node.state as XcfaState<*>).processes.forEach { (pid: Int, proc: XcfaProcessState) ->
          if (proc != (candidate.state as XcfaState<*>).processes[pid]) {
            if (popCovered) proc.popped = proc.locs.pop()
            onlyStackCovers = true
          }
        }
        if (!onlyStackCovers) {
          node.cover(candidate)
        }
        return
      }
    }
  }

  companion object {

    fun <S : State, A : Action, P : Prec> builder(
      argBuilder: ArgBuilder<S, A, P>
    ): BasicArgAbstractor.Builder<S, A, P> {
      return Builder(argBuilder)
    }
  }

  class Builder<S : State, A : Action, P : Prec>(argBuilder: ArgBuilder<S, A, P>) :
    BasicArgAbstractor.Builder<S, A, P>(argBuilder) {

    override fun build(): BasicArgAbstractor<S, A, P> {
      return XcfaArgAbstractor(argBuilder, projection, waitlist, stopCriterion, logger)
    }
  }
}
