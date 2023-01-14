package pers.neige.neigeitems.utils

import com.alibaba.fastjson2.parseObject
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.ConfigurationSection
import pers.neige.neigeitems.manager.SectionManager
import pers.neige.neigeitems.section.Section
import taboolib.module.nms.ItemTag
import taboolib.module.nms.ItemTagData
import taboolib.module.nms.ItemTagType
import java.awt.Color
import java.util.*

/**
 * 节点相关工具类
 */
object SectionUtils {
    /**
     * 对文本进行节点解析
     *
     * @param cache 解析值缓存
     * @param player 待解析玩家
     * @param sections 节点池
     * @return 解析值
     */
    @JvmStatic
    fun String.parseSection(
        cache: HashMap<String, String>? = null,
        player: OfflinePlayer? = null,
        sections: ConfigurationSection? = null
    ): String {
        // 定位最外层 <> 包裹的字符串
        val start = ArrayList<Int>()
        val end = ArrayList<Int>()
        load(this, start, end)
        if (start.size == 0) return this
        // 对 <> 包裹的文本进行节点解析
        val listString = StringBuilder(this.substring(0, start[0]))
        for (index in 0 until start.size) {
            // 解析目标文本
            listString.append(
                // 先截取文本
                this.substring(start[index]+1, end[index])
                    // 因为取的是最外层 <> 包裹的字符串, 所以内部可能还需要继续解析
                    .parseSection(cache, player, sections)
                    // 解析完成后可以视作节点ID/即时声明节点, 进行节点调用
                    .getSection(cache, player, sections)
            )

            if (index+1 != start.size) {
                listString.append(this.substring(end[index]+1, start[(start.size-1).coerceAtMost(index+1)]))
            } else {
                listString.append(this.substring(end[index]+1, this.length))
            }
        }
        return listString.toString()
    }

    /**
     * 对文本进行节点解析
     *
     * @return 解析值
     */
    @JvmStatic
    fun String.parseSection(): String {
        return this.parseSection(null, null, null)
    }

    /**
     * 对文本进行节点解析
     *
     * @param cache 解析值缓存
     * @return 解析值
     */
    @JvmStatic
    fun String.parseSection(cache: HashMap<String, String>?): String {
        return this.parseSection(cache, null, null)
    }

    /**
     * 对文本进行节点解析
     *
     * @param player 待解析玩家
     * @return 解析值
     */
    @JvmStatic
    fun String.parseSection(player: OfflinePlayer?): String {
        return this.parseSection(null, player, null)
    }

    /**
     * 对文本进行节点解析
     *
     * @param cache 解析值缓存
     * @param sections 节点池
     * @return 解析值
     */
    @JvmStatic
    fun String.parseSection(cache: HashMap<String, String>?, sections: ConfigurationSection?): String {
        return this.parseSection(cache, null, sections)
    }

    /**
     * 对文本进行节点解析
     *
     * @param parse 是否对文本进行节点解析
     * @param cache 解析值缓存
     * @param player 待解析玩家
     * @param sections 节点池
     * @return 解析值
     */
    @JvmStatic
    fun String.parseSection(
        parse: Boolean,
        cache: HashMap<String, String>? = null,
        player: OfflinePlayer? = null,
        sections: ConfigurationSection? = null
    ): String {
        return when {
            parse -> this.parseSection(cache, player, sections)
            else -> this
        }
    }

    /**
     * 对节点内容进行解析 (已经去掉 <>)
     *
     * @param cache 解析值缓存
     * @param player 待解析玩家
     * @param sections 节点池
     * @return 解析值
     */
    @JvmStatic
    fun String.getSection(
        cache: HashMap<String, String>?,
        player: OfflinePlayer?,
        sections: ConfigurationSection?
    ): String {
        val string = this
            .replace("\\<", "<")
            .replace("\\>", ">")
        when (val index = string.indexOf("::")) {
            // 私有节点调用
            -1 -> {
                // 尝试读取缓存
                if (cache?.get(string) != null) {
                    // 直接返回对应节点值
                    return cache[string] as String
                // 读取失败, 尝试主动解析
                } else {
                    // 尝试解析并返回对应节点值
                    if (sections != null && sections.contains(string)) {
                        // 获取节点ConfigurationSection
                        val section = sections.getConfigurationSection(string)
                        // 简单节点
                        if (section == null) {
                            val result = sections.getString(string)?.parseSection(cache, player, sections) ?: "<$string>"
                            cache?.put(string, result)
                            return result
                        }
                        // 加载节点
                        return Section(section, string).load(cache, player, sections) ?: "<$string>"
                    }
                    if (string.startsWith("#")) {
                        try {
                            try {
                                val hex = (string.substring(1).toIntOrNull(16) ?: 0)
                                    .coerceAtLeast(0)
                                    .coerceAtMost(0xFFFFFF)
                                val color = Color(hex)
                                return ChatColor.of(color).toString()
                            } catch (error: NumberFormatException) {}
                        } catch (error: NoSuchMethodError) {
                            Bukkit.getLogger().info("§e[NI] §6低于1.16的版本不能使用16进制颜色哦")
                        }
                    }
                }
                return "<$string>"
            }
            // 即时声明节点解析
            else -> {
                // 获取节点类型
                val type = string.substring(0, index)
                // 获取参数
                val args = string.substring(index+2).split("(?<!\\\\)_".toRegex()).map { it.replace("\\_", "_") }
                return SectionManager.sectionParsers[type]?.onRequest(args, cache, player, sections) ?: "<$string>"
            }
        }
    }

    /**
     * 对文本进行物品节点解析
     *
     * @param itemTag 物品NBT
     * @return 解析值
     */
    @JvmStatic
    fun String.parseItemSection(itemTag: ItemTag, player: OfflinePlayer): String {
        // 定位最外层 <> 包裹的字符串
        val start = ArrayList<Int>()
        val end = ArrayList<Int>()
        load(this, start, end)
        if (start.size == 0) return this
        // 对 <> 包裹的文本进行节点解析
        val listString = StringBuilder(this.substring(0, start[0]))
        for (index in 0 until start.size) {
            // 解析目标文本
            listString.append(
                this.substring(start[index]+1, end[index])
                    .parseItemSection(itemTag, player)
                    .getItemSection(itemTag, player)
            )

            if (index+1 != start.size) {
                listString.append(this.substring(end[index]+1, start[(start.size-1).coerceAtMost(index+1)]))
            } else {
                listString.append(this.substring(end[index]+1, this.length))
            }
        }
        return listString.toString()
    }

    /**
     * 对物品节点内容进行解析 (已经去掉 <>)
     *
     * @param itemTag 物品NBT
     * @return 解析值
     */
    @JvmStatic
    fun String.getItemSection(itemTag: ItemTag, player: OfflinePlayer): String {
        val string = this
            .replace("\\<", "<")
            .replace("\\>", ">")
        when (val index = string.indexOf("::")) {
            -1 -> {
                return "<$string>"
            }
            else -> {
                val name = string.substring(0, index)
                val args = string.substring(index + 2)
                return when (name.lowercase(Locale.getDefault())) {
                    "nbt" -> {
                        var value: ItemTagData = itemTag
                        val argsArray: Array<String> = args.split(".").toTypedArray()

                        argsArray.forEach { key ->
                            when (value.type) {
                                ItemTagType.LIST -> {
                                    key.toIntOrNull()?.let { index ->
                                        val list = value.asList()
                                        if (list.size > index) {
                                            value.asList()[index.coerceAtLeast(0)].also { value = it } ?: let { value = ItemTagData("<$string>") }
                                        } else { value = ItemTagData("<$string>") }
                                    } ?: let { value = ItemTagData("<$string>") }
                                }
                                ItemTagType.COMPOUND -> value.asCompound()[key]?.also { value = it } ?: let { value = ItemTagData("<$string>") }
                                else -> let { value = ItemTagData("<$string>") }
                            }
                        }

                        return value.asString()
                    }
                    "data" -> {
                        val data = itemTag["NeigeItems"]?.asCompound()?.get("data")?.asString()
                        data?.parseObject<HashMap<String, String>>()?.get(args) ?: "<$string>"
                    }
                    else -> {
                        return SectionManager.sectionParsers[name]?.onRequest(args.split("_"), null, player) ?: "<$string>"
                    }
                }
            }
        }
    }

    /**
     * 加载字符串最外层<>位置
     *
     * @param string 待加载文本
     * @param start 用于存储<位置
     * @param end 用于存储>位置
     */
    private fun load(string: String, start: ArrayList<Int>, end: ArrayList<Int>) {
        val stack = LinkedList<Int>()
        string.forEachIndexed { index, char ->
            if (char == '<' && string[0.coerceAtLeast(index - 1)] != '\\') {
                // 压栈
                stack.push(index)
                // 如果是右括号
            } else if (char == '>' && string[0.coerceAtLeast(index - 1)] != '\\') {
                // 前面有左括号了
                if (!stack.isEmpty()) {
                    // 还不止一个
                    if (stack.size > 1) {
                        // 出栈
                        stack.pop()
                        // 只有一个
                    } else {
                        // 记录并出栈
                        start.add(stack.poll())
                        end.add(index)
                    }
                }
            }}
    }
}