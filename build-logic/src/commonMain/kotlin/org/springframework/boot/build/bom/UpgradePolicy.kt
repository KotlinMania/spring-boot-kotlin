/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.build.bom

import org.springframework.boot.build.bom.bomr.version.DependencyVersion
import java.util.function.BiPredicate

/**
 * Policies used to decide which versions are considered as possible upgrades.
 * 
 * @author Andy Wilkinson
 */
enum class UpgradePolicy(private val delegate: BiPredicate<DependencyVersion?, DependencyVersion?>) :
    BiPredicate<DependencyVersion?, DependencyVersion?> {
    /**
     * Any version.
     */
    ANY(BiPredicate { candidate: DependencyVersion?, current: DependencyVersion? -> true }),

    /**
     * Minor versions of the current major version.
     */
    SAME_MAJOR_VERSION(BiPredicate { obj: DependencyVersion?, other: DependencyVersion? -> obj!!.isSameMajor(other) }),

    /**
     * Patch versions of the current minor version.
     */
    SAME_MINOR_VERSION(BiPredicate { obj: DependencyVersion?, other: DependencyVersion? -> obj!!.isSameMinor(other) });

    override fun test(candidate: DependencyVersion?, current: DependencyVersion?): Boolean {
        return this.delegate.test(candidate, current)
    }

    companion object {
        fun max(one: UpgradePolicy?, two: UpgradePolicy?): UpgradePolicy {
            if (one == null && two != null) {
                return two
            } else if (one != null && two == null) {
                return one
            }
            return (if (one.ordinal < two.ordinal) one else two)!!
        }
    }
}
