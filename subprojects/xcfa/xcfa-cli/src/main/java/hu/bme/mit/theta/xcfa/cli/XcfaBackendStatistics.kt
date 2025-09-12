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
package hu.bme.mit.theta.xcfa.cli

import hu.bme.mit.theta.analysis.algorithm.Statistics
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap;

class XcfaBackendStatistics : Statistics() {
    companion object {
        private var backendTimesMs = ConcurrentHashMap<String, MutableList<Long>>()

        fun addTime(key: String, time: Long) {
          // if (backendTimesMs.containsKey(key)) {
          //     backendTimesMs[key]?.add(time)
          // } else {
          //   backendTimesMs[key] = mutableListOf(time);
          // }
            backendTimesMs.getOrPut(key) {
                Collections.synchronizedList(mutableListOf())
            }.add(time)
        }

        fun clear() {
            backendTimesMs = ConcurrentHashMap()
        }

        fun getCopy(): Map<String, List<Long>> {
            return backendTimesMs.mapValues { (_, list) ->
                synchronized(list) {
                    list.toList()
                }
            }
        }
    }

    init {
      val timeCopy = getCopy();
      addStat("backendTimesMs") { timeCopy }
      // WARN: Don't do addStat("backendTimesMs") { getCopy() }
      // because getCopy() will only be called then that other
      // funtion is called
    }
}


// fun withBackendTime(item: Long): XcfaBackendStatistics {
//   backendTimesMs.add(item)
//   return this
// }
//
// fun withBackendTimes(items: List<Long>): XcfaBackendStatistics {
//   backendTimesMs.addAll(items)
//   return this
// }
  

// class XcfaBackendStatistics(val backendTimesMs: mutableListOf<mutableListOf<Long>>) : Statistics() {
//
//   init {
//     addStat("backendTimesMs", this::backendTimesMs)
//   }
//
//   fun withBackendTimeStat(item: Long, depth: Int /*0 index*/) {
//     if (depth < 0) error("You cant have negative depth!")
//     
//     val currentDepth = backendTimesMs.size()-1
//     val diff = depth - currentDepth;
//     if (diff == 1) {
//       addStat(backendTimesMs.add(mutableListOf(item)));
//     } else if (diff > 1) {
//       error("[ERROR] When adding a new item (time) in the XcfaBackendStatistics, " +
//             "you skipped depth levels. Current max depth: $currentDepth, " +
//             "but you tried to add at depth: $depth")
//     } else if (diff < 1) {
//       addStat(backendTimesMs[depth].add(item));
//     } else {
//       error("Unknow Error!")
//     } 
//     return this
//   }
//
// }

