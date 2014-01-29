(ns naojure.sample
  (:require [naojure.core :as nao :refer []]))

;; examples of how we'd like to control NAO with clojure

;; need to be careful that nap/do does not hide built-in do
;; call it donao by default unless people alias it?


;; update with IP address
(def robot (nao/make-robot "127.0.0.1"))

(nao/say robot "Hello clojure")

;; complete movement in default time
(nao/donao
 (arms :out )
 (hands :open))

;; complete movement in 0.1 second
(nao/do 0.1
          (arms :out)
          (hands :left_open)
          (hands :right_close))

(nao/do chan
 (arms :out )
 (hands :open)
 (leds :white))

nao/do returns a channel which a message is sent on when the action completes

variants
- actions
- time actions
- chan actions
- time chan actions

(nao/add-chan robot :event chan)
(nao/remove-chan robot :event chan)
(nao/remove-all-chan robot :event)

(nao/add-callback robot :event fn)
(nao/remove-callback robot :event fn)
(nao/clear-callback robot :event)

(def exterminate "file://somewhere in nao filesystem")
- get exterminate sound sample
