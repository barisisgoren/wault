package app.wault.data

object Reorder {
    /** Listeyi verilen id sırasına göre dizer; sırada olmayan kartlar sona, eski sıralarıyla eklenir. */
    fun <T : Any> applyOrder(list: List<T>, ids: List<String>, idOf: (T) -> String): List<T> {
        val byId = list.associateBy(idOf)
        val ordered = ids.mapNotNull { byId[it] }
        val rest = list.filter { idOf(it) !in ids.toSet() }
        return ordered + rest
    }

    fun applyOrder(list: List<WaultCard>, ids: List<String>): List<WaultCard> =
        applyOrder(list, ids) { it.id }

    /** Bir elemanı from konumundan to konumuna taşır. */
    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from == to || from !in list.indices) return list
        val m = list.toMutableList()
        val item = m.removeAt(from)
        m.add(to.coerceIn(0, m.size), item)
        return m
    }
}
