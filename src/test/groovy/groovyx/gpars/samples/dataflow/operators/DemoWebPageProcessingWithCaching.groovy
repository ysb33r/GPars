// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.dataflow.operators

import groovyx.gpars.dataflow.DataFlowQueue
import static groovyx.gpars.dataflow.DataFlow.operator
import static groovyx.gpars.dataflow.DataFlow.prioritySelector
import static groovyx.gpars.dataflow.DataFlow.splitter
import static groovyx.gpars.dataflow.DataFlow.task

/**
 * Builds a network of dataflow operators, which will in turn complete provided urls, download them, search for the words
 * 'groovy' and 'scala' in them and returning reports telling, which site refers to which of the two languages.
 *
 * Uses advanced techniques to speed-up information retrieval using speculation and confirmation technique,
 * described by Greg Barish in his paper (see http://www.jroller.com/vaclav/entry/speculate_on_information_in_parallel)
 *
 * @author Vaclav Pech
 * Date 23rd Sep 2010
 */

final DataFlowQueue urlsRequests = new DataFlowQueue()
final DataFlowQueue urls = new DataFlowQueue()
final DataFlowQueue urlsForSpeculation = new DataFlowQueue()
final DataFlowQueue pages = new DataFlowQueue()
final DataFlowQueue downloadedPages = new DataFlowQueue()
final DataFlowQueue speculativePages = new DataFlowQueue()
final DataFlowQueue pagesForGroovy = new DataFlowQueue()
final DataFlowQueue pagesForScala = new DataFlowQueue()
final DataFlowQueue resultsFromGroovy = new DataFlowQueue()
final DataFlowQueue resultsFromScala = new DataFlowQueue()
final DataFlowQueue unconfirmedReports = new DataFlowQueue()
final DataFlowQueue approvals = new DataFlowQueue()
final DataFlowQueue reports = new DataFlowQueue()
final DataFlowQueue contentForCache = new DataFlowQueue()

def long counter = 0L
def urlResolver = operator(inputs: [urlsRequests], outputs: [urls, urlsForSpeculation]) {
    bindAllOutputs([id: counter++, url: "http://www.${it}.com"])
}

def downloader = operator(inputs: [urls], outputs: [downloadedPages, contentForCache], maxForks: 4) {
    try {
        def content = it.url.toURL().text
        it.content = content
    } catch (ignore) {
        it.content = 'Could not download'
    }
    bindAllOutputsAtomically it
}

def cache = ['http://www.jetbrains.com': 'groovy scala kfjhskfhsk']

def speculator = prioritySelector(inputs: [urlsForSpeculation, contentForCache], outputs: [speculativePages]) {msg, index ->
    if (index == 0) {
        def content = cache[msg.url]
        if (content) bindAllOutputs([id: msg.id, url: msg.url, speculation: true, content: content])
    } else {
        cache[msg.url] = msg.content
    }
}

def unconfirmedSpeculations = [:]
def processedIds = new HashSet()

def evaluator = prioritySelector(inputs: [downloadedPages, speculativePages], outputs: [pages, approvals]) {msg, index ->
    if (msg.id in processedIds) return  //ignore all messages for finished ids
    if (index == 0) {
        processedIds << msg.id
        final Object speculativeReport = unconfirmedSpeculations.remove(msg.id)
        if (speculativeReport) {
            if (compareSpeculationWithRealContent(speculativeReport, msg.content)) {
                bindOutput 1, [id: msg.id]  //confirm the earlier speculation
            } else {
                bindOutput 0, msg  //speculation turned out to be false, send the real content along
            }
        } else {
            bindOutput msg  //no speculation has been attempted, send on the real content
        }
    } else {
        assert msg.speculation
        unconfirmedSpeculations[msg.id] = msg.content
        bindOutput msg  //send the speculation on
    }
}

def splitter = splitter(pages, [pagesForGroovy, pagesForScala])

def groovyScanner = operator(pagesForGroovy, resultsFromGroovy) {
    def foundWord = it.content.toLowerCase().contains('groovy') ? 'groovy' : ''
    bindOutput([id: it.id, url: it.url, foundWord: foundWord, speculation: it.speculation])
}

def scalaScanner = operator(pagesForScala, resultsFromScala) {
    def foundWord = it.content.toLowerCase().contains('scala') ? 'scala' : ''
    bindOutput([id: it.id, url: it.url, foundWord: foundWord, speculation: it.speculation])
}

def reporter = operator(inputs: [groovyScanner.output, scalaScanner.output], outputs: [unconfirmedReports], maxForks: 4) {g, s ->
    assert g.url == s.url
    assert g.id == s.id
    assert g.speculation == s.speculation
    def words = [g.foundWord, s.foundWord].findAll {it}
    def result
    switch (words.size()) {
        case 2:
            result = "${g.foundWord} and ${s.foundWord}"
            break
        case 1:
            result = words[0]
            break
        default:
            result = 'No interesting words'
    }
    bindOutput([id: g.id, speculation: g.speculation, result: ("$result found at ${g.url}" + (g.speculation ? ' based on speculation' : ''))])
}

def unconfirmedSpeculativeReports = [:]
def deliveredConfirmations = [:]

def confirm = prioritySelector(inputs: [approvals, unconfirmedReports], outputs: [reports]) {msg, index ->
    if (index == 1) {
        if (msg.speculation) {
            if (deliveredConfirmations[msg.id]) {
                bindOutput msg.result
                deliveredConfirmations.remove(msg.id)
            } else {
                unconfirmedSpeculativeReports[msg.id] = msg
            }
        }
        else {
            bindOutput msg.result
            unconfirmedSpeculativeReports.remove(msg.id)
            deliveredConfirmations.remove(msg.id)
        }
    } else {
        final Object speculativeReport = unconfirmedSpeculativeReports[msg.id]
        if (speculativeReport) {
            bindOutput speculativeReport.result
            unconfirmedSpeculativeReports.remove(msg.id)
        } else {
            deliveredConfirmations[msg.id] = msg
        }
    }
}

private boolean compareSpeculationWithRealContent(content1, content2) {
    return content1?.size() == content2?.size()
}

task {
    final Object incomingReports = confirm.output
    for (;;) {
        println incomingReports.val
    }
}

['dzone', 'infoq', 'jetbrains', 'oracle'].each {
    urlsRequests << it
}

sleep 10000
println 'Cache ' + cache.keySet()

['dzone', 'infoq', 'invalidUrl_', 'jetbrains', 'oracle'].each {
    urlsRequests << it
}

sleep 10000