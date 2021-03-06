/*
 * Copyright © 2002-2019 Neo4j Sweden AB (http://neo4j.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.v9_0.rewriting

import org.opencypher.v9_0.ast.AstConstructionTestSupport
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.rewriting.rewriters.replaceAliasedFunctionInvocations
import org.opencypher.v9_0.util.InputPosition
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite

class ReplaceAliasedFunctionInvocationsTest extends CypherFunSuite with AstConstructionTestSupport {

  private val rewriter = replaceAliasedFunctionInvocations(Deprecations.V1)
  private val deprecatedNameMap = Map(
    "toInt" -> "toInteger",
    "upper" -> "toUpper",
    "lower" -> "toLower",
    "rels" -> "relationships")

  test("should rewrite deprecated names regardless of casing") {
    for ((oldName, newName) <- deprecatedNameMap ) {
      rewriter(function(oldName, varFor("arg"))) should equal(function(oldName, varFor("arg")).copy(functionName = FunctionName(newName)(pos))(pos))
      rewriter(function(oldName.toLowerCase(), varFor("arg"))) should equal(function(newName, varFor("arg")))
      rewriter(function(oldName.toUpperCase(), varFor("arg"))) should equal(function(newName, varFor("arg")))
    }
  }

  test("should not touch new names of regardless of casing") {
    for (newName <- deprecatedNameMap.values ) {
      rewriter(function(newName, varFor("arg"))) should equal(function(newName, varFor("arg")))
      rewriter(function(newName.toLowerCase(), varFor("arg"))) should equal(function(newName, varFor("arg")))
      rewriter(function(newName.toUpperCase(), varFor("arg"))) should equal(function(newName, varFor("arg")))
    }
  }

  test("should rewrite timestamp()") {
    val before = function("timestamp")

    val after = prop(function("datetime"), "epochMillis")
    rewriter(before) should equal(after)
  }

  test("should also rewrite TiMeStAmP()") {
    val before = function("TiMeStAmP")

    val after = prop(function("datetime"), "epochMillis")
    rewriter(before) should equal(after)
  }

  test("should rewrite extract() in V2") {
    val before = ExtractExpression(ExtractScope(varFor("a"), None, None)(pos),
                                   literalFloat(3.0))(pos)
    val expected = listComprehension("a", literalFloat(3.0), None, None)

    replaceAliasedFunctionInvocations(Deprecations.V1)(before) should equal(before)
    replaceAliasedFunctionInvocations(Deprecations.V2)(before) should equal(expected)
  }

  test("should rewrite filter() in V2") {
    val scopePosition = InputPosition(30, 1, 31)
    val before = FilterExpression(FilterScope(varFor("a"), Some(trueLiteral))(scopePosition),
                                  literalFloat(3.0))(pos)
    val expected = ListComprehension(ExtractScope(varFor("a"), Some(trueLiteral), None)(scopePosition),
                                     literalFloat(3.0))(pos)

    replaceAliasedFunctionInvocations(Deprecations.V1)(before) should equal(before)
    replaceAliasedFunctionInvocations(Deprecations.V2)(before) should equal(expected)
  }

}
