package dev.kikugie.stwotcher.exec.issue

import dev.kikugie.stwotcher.data.token.StitcherToken
import java.nio.file.Path

class FileProblemsBuilder(val file: Path, val source: CharSequence) {
    val storage: MutableList<ProblemEntry> = mutableListOf()

    inline fun report(id: ProblemID, token: StitcherToken, action: ProblemEntry.() -> Unit = {}) =
        report(ProblemEntry(id, token.range).apply(action))
    inline fun report(id: ProblemID, index: Int, action: ProblemEntry.() -> Unit = {}) =
        report(ProblemEntry(id, index ..< index).apply(action))
    inline fun report(id: ProblemID, range: IntRange, action: ProblemEntry.() -> Unit = {}) =
        report(ProblemEntry(id, range).apply(action))

    fun report(entry: ProblemEntry): ProblemEntry {
        storage += entry; return entry
    }
}

