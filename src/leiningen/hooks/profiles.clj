(ns leiningen.profiles
  (:require [robert.hooke]
            [clojure.set :as set]
            [clojure.java.io :as io]))

;; Modified merge-with to provide f with the conflicting key.
(defn- merge-with-key [f & maps]
  (when (some identity maps)
    (let [merge-entry (fn [m e]
                        (let [k (key e) v (val e)]
                          (if (contains? m k)
                            (assoc m k (f k (get m k) v))
                            (assoc m k v))))
          merge2 (fn [m1 m2]
                   (reduce merge-entry (or m1 {}) (seq m2)))]
      (reduce merge2 maps))))

;; TODO: This would just be a merge if we had an ordered map
(defn- merge-dependencies [result latter]
  (let [latter-deps (set (map first latter))]
    (concat latter (remove (comp latter-deps first) result))))

(defn- profile-key-merge
  "Merge profile values into the project map based on their type."
  [key result latter]
  (cond (-> result meta :displace)
        latter

        (-> latter meta :replace)
        latter

        (= :dependencies key)
        (merge-dependencies result latter)

        (= :repositories key)
        (concat (seq result) (seq latter))

        (and (map? result) (map? latter))
        (merge-with-key profile-key-merge latter result)

        (and (set? result) (set? latter))
        (set/union latter result)

        (and (coll? result) (coll? latter))
        (concat latter result)

        :else (doto latter (prn :profile-merge-else))))

(defn- merge-profile [project profile]
  (merge-with-key profile-key-merge project profile))

(defn- lookup-profile [profiles profile]
  (let [result (profiles profile)]
    (if (keyword? result)
      (recur profiles result)
      result)))

(defn user-profiles []
  ;; require at runtime so the hook won't explode in 2.x
  (require 'leiningen.util.paths)
  (let [profiles-file (io/file ((resolve 'leiningen.util.paths/leiningen-home))
                               "profiles.clj")]
    (if (.exists profiles-file)
      (read-string (slurp profiles-file)))))

(defn- profiles-for
  "Read profiles from a variety of sources.

  We check the profiles.clj file in ~/.lein/profiles.clj and
  the :profiles key from the project map."
  [project profiles-to-apply]
  (when (some (comp :repositories val) (user-profiles))
    (println "WARNING: :repositories detected in user-level profile!")
    (println "See https://github.com/technomancy/leiningen/wiki/Repeatability"))
  (let [profiles (merge (user-profiles) (:profiles project))]
    ;; We reverse because we want profile values to override the
    ;; project, so we need "last wins" in the reduce, but we want the
    ;; first profile specified by the user to take precedence.
    (map (partial lookup-profile profiles) (reverse profiles-to-apply))))

(defn merge-profiles
  "Look up and merge the given profile names into the project map."
  [project profiles-to-apply]
  (with-meta (reduce merge-profile project
                     (profiles-for project profiles-to-apply))
    {:without-profiles project}))

(defn get-profiles []
  (map keyword (.split (or (System/getenv "LEIN_PROFILE") "dev,user") ",")))

(when (.startsWith (System/getenv "LEIN_VERSION") "1.")
  (robert.hooke/add-hook (resolve 'leiningen.core/read-project)
                         (fn [f & args]
                           (merge-profiles (apply f args) (get-profiles)))))