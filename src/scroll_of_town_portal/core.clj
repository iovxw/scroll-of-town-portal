(ns scroll-of-town-portal.core
  (:import [org.bukkit Bukkit Material ChatColor]
           [org.bukkit.inventory ItemStack ShapelessRecipe]
           [org.bukkit.enchantments Enchantment]
           [org.bukkit.event.block Action])
  (:gen-class :name scroll_of_town_portal.core
              :extends org.bukkit.plugin.java.JavaPlugin
              :implements [org.bukkit.event.Listener]
              :methods [[^{org.bukkit.event.EventHandler true}
                         onPlayerUse [org.bukkit.event.player.PlayerInteractEvent] void]]))

(def logger (atom nil))
(def tp-item (atom nil))

(defn log-info [msg]
  (.info @logger msg))
(defn log-config [msg]
  (.config @logger msg))
(defn log-waring [msg]
  (.waring @logger msg))
(defn log-fine [msg]
  (.fine @logger msg))
(defn log-finer [msg]
  (.finer @logger msg))
(defn log-finest [msg]
  (.finest @logger msg))
(defn log-severe [msg]
  (.severe @logger msg))
(defn log-throwing [source-class source-method thrown]
  (.throwing @logger source-class source-method thrown))

(defn -onEnable [this]
  (reset! logger (.getLogger this))
  (reset! tp-item (let [item (ItemStack. Material/PAPER)
                        meta (-> (Bukkit/getItemFactory)
                                 (.getItemMeta Material/PAPER))]
                    (.setDisplayName meta "回城卷轴")
                    (.addEnchant meta Enchantment/DURABILITY 1 false)
                    (.setLore meta [(str ChatColor/RED "消耗品")
                                    "右键使用即可传送回床"])
                    (.setItemMeta item meta)
                    item))
  (let [sr (ShapelessRecipe. @tp-item)]
    (.addIngredient sr Material/PAPER)
    (.addIngredient sr Material/EMERALD)
    (-> this
        .getServer
        (.addRecipe sr)))
  (-> this
      .getServer
      .getPluginManager
      (.registerEvents this this)))

(defn -onDisable [this]
  (-> this
      .getServer
      .clearRecipes))

(defn -onPlayerUse [self event]
  ; 手上拿着卷轴，同时右键了方块或空气
  (when (and (.isSimilar @tp-item (.getItem event))
             (or (= (.getAction event) Action/RIGHT_CLICK_AIR)
                 (= (.getAction event) Action/RIGHT_CLICK_BLOCK)))
    (let [player (.getPlayer event)
          item (.getItem event)
          inv (.getInventory player)
          bed (.getBedSpawnLocation player)
          in-main-hand? (.equals item (.getItemInMainHand inv))
          off-hand-has-tp-item? (.isSimilar @tp-item (.getItemInOffHand inv))]
      ; 如果现在使用的是是主手，而且副手也有卷轴，那么不做任何动作
      ; 因为这个Event会同时传给主手和副手，为了防止同时使用
      (when-not (and in-main-hand? off-hand-has-tp-item?)
        (if bed
          (do (.teleport player bed)
              (let [ritem (.clone item)]
                (.setAmount ritem (dec (.getAmount ritem)))
                (if in-main-hand?
                  (.setItemInMainHand inv ritem)
                  (.setItemInOffHand inv ritem))))
          (.sendMessage player (str ChatColor/RED "传送失败: "
                                    ChatColor/WHITE
                                    "没有找到床")))))))
