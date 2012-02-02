# lein-profiles

Profiles, backported from Leiningen 2 for your enjoyment.

## Usage

    $ lein plugin install lein-profiles 0.1.0
    
    ```clj
    ;; Add this to ~/.lein/init.clj
    (try (require 'leiningen.hooks.profiles)
         (catch java.io.FileNotFoundException _))
     ```

For details on how profiles work, see the README for Leiningen 2. By
default the :dev and :user profiles are active; this can be changed
with the `LEIN_PROFILE` environment variable. The `with-profiles` task
is not ported.

Disclaimer: this may not actually be a good idea. It might be better
to just use Leiningen 2. Only time will tell!

## License

Copyright © 2012 Phil Hagelberg

Distributed under the Eclipse Public License, the same as Clojure.
