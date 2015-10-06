(ns clojure-pl.lisp-test
  (:require [acolfut.sweet :refer :all]
            [clojure-pl.cota :refer :all]
            [clojure-pl.lisp.interpreter.parser :refer :all]
            [clojure-pl.lisp.interpreter.buildin :refer :all]
            [clojure-pl.lisp.interpreter.interp :refer :all]))

(reset! *debug* false)

(deftest interpreter-test
  (testing "tokenizer test"
    (is= (#'clojure-pl.lisp.interpreter.parser/tokenizer "(cleantha)")
         [[:open] [:symbol "cleantha"] [:close]])
    (is= (#'clojure-pl.lisp.interpreter.parser/tokenizer "(\"cleantha\")")
         [[:open] [:string "cleantha"] [:close]])
    (is= (#'clojure-pl.lisp.interpreter.parser/tokenizer "+12")
         [[:number "+12"]])
    (is= (#'clojure-pl.lisp.interpreter.parser/tokenizer "-12")
         [[:number "-12"]])
    (is= (#'clojure-pl.lisp.interpreter.parser/tokenizer "12")
         [[:symbol "12"]])
    (is= (#'clojure-pl.lisp.interpreter.parser/tokenizer "(+ 1 1)")
         [[:open] [:symbol \+] [:symbol \1] [:symbol \1] [:close]])
    (is= (#'clojure-pl.lisp.interpreter.parser/tokenizer "(+ (+ 1 2) 1)")
         [[:open] [:symbol \+] [:open] [:symbol \+] [:symbol \1] [:symbol \2] [:close] [:symbol \1] [:close]])
    (is= (#'clojure-pl.lisp.interpreter.parser/tokenizer "symbol")
         [[:symbol "symbol"]]))
  (testing "parse test"
    (is= (#'clojure-pl.lisp.interpreter.parser/parse "(+ 1 a)") [:+ 1.0 :a])
    (is= (#'clojure-pl.lisp.interpreter.parser/parse "+12") 12.0)
    (is= (#'clojure-pl.lisp.interpreter.parser/parse "\"cleantha\"") "cleantha")
    (is= (#'clojure-pl.lisp.interpreter.parser/parse "\"clean\\\"tha\"") "clean\"tha")
    (is= (#'clojure-pl.lisp.interpreter.parser/parse "kalle olle") :kalle)
    (is= (#'clojure-pl.lisp.interpreter.parser/parse "(kalle +12 24) (olle -12)") [:kalle 12.0 24.0])
    (is= (#'clojure-pl.lisp.interpreter.parser/parse "(not true)") [:not :true]))
  (testing "eval test"
    ;;primitive
    (is= (eval* (parse "(+ 1 2)") buildin-env) [3.0 buildin-env])
    (is= (eval* (parse "(> 1 2)") buildin-env) [false buildin-env])
    (is= (eval* (parse "(+ 1 2 3)") buildin-env) [6.0 buildin-env])
    ;;not
    (is= (eval* (parse "(not true)") buildin-env) [(not true) buildin-env])
    ;;if
    (is= (eval* (parse "(if true 1 2)") buildin-env) [1.0 buildin-env])
    (is= (eval* (parse "(if false 1 2)") buildin-env) [2.0 buildin-env])
    ;;cond
    (is= (eval* (parse "(cond (true 1))") buildin-env) [1.0 buildin-env])
    (is= (eval* (parse "(cond (false 1) (true 2))") buildin-env) [2.0 buildin-env])
    (is= (eval* (parse "(cond (false 1) (false 2) (else 3))") buildin-env) [3.0 buildin-env])
    ;;cons
    (is= (eval* (parse "(cons 1)") buildin-env) ['(1.0) buildin-env])
    (is= (eval* (parse "(cons 1 2)") buildin-env) ['(1.0 2.0) buildin-env])
    (is= (eval* (parse "(cons 1 (1 2))") buildin-env) ['(1.0 1.0 2.0) buildin-env])
    (is= (eval* (parse "(cons 1 (cons 1 2))") buildin-env) ['(1.0 1.0 2.0) buildin-env])
    ;;list
    (is= (eval* (parse "(list 1 2)") buildin-env) ['(1.0 2.0) buildin-env])
    (is= (eval* (parse "(list (+ 1 2) (if (> 1 2) 1 2))") buildin-env) ['(3.0 2.0) buildin-env])
    ;;append
    (is= (eval* (parse "(append (1 2) (3 4))") buildin-env) ['(1.0 2.0 3.0 4.0) buildin-env])
    (is= (eval* (parse "(append ((+ 1 2) 2) (3 4))") buildin-env) ['(3.0 2.0 3.0 4.0) buildin-env])
    ;;begin
    (is= (eval* (parse "(begin (+ 1 2))") buildin-env) [3.0 buildin-env])
    (is= (eval* (parse "(begin (+ 1 2) (+ 3 4))") buildin-env) [7.0 buildin-env])
    ;;car
    (is= (eval* (parse "(car ())") buildin-env) [nil buildin-env])
    (is= (eval* (parse "(car (1))") buildin-env) [1.0 buildin-env])
    (is= (eval* (parse "(car (1 2 3))") buildin-env) [1.0 buildin-env])
    ;;cdr
    (is= (eval* (parse "(cdr ())") buildin-env) ['() buildin-env])
    (is= (eval* (parse "(cdr (1))") buildin-env) ['() buildin-env])
    (is= (eval* (parse "(cdr (1 2))") buildin-env) ['(2.0) buildin-env])
    (is= (eval* (parse "(cdr (1 2 3))") buildin-env) ['(2.0 3.0) buildin-env])
    ;;null?
    (is= (eval* (parse "(null? nil)") buildin-env) [true buildin-env])
    (is= (eval* (parse "(null? 1)") buildin-env) [false buildin-env])
    (is= (eval* (parse "(null? (if true 1))") buildin-env) [false buildin-env])
    (is= (eval* (parse "(null? (if false 1))") buildin-env) [true buildin-env])
    ;;let
    (is= (eval* (parse "(let ((a 1) (b 2)) (+ a b))") buildin-env) [3.0 buildin-env])
    ;;display newline
    (is= (eval* (parse "(display (+ 1 2))") buildin-env) [nil buildin-env])
    (is= (eval* (parse "(newline)") buildin-env) [nil buildin-env])
    ;;define
    (is= (eval* (parse "(add 1 2)") (second (eval* (parse "(define (add a b) (+ a b))") buildin-env)))
         [3.0 (cons {:a 1.0 :b 2.0} (assoc-in buildin-env [0 :add] '((:a :b) [:+ :a :b])))])
    (is= (eval* (parse "(+ a 2)") (second (eval* (parse "(define a 1)") buildin-env)))
         [3.0 (assoc-in buildin-env [0 :a] 1.0)])
    ;;lambda
    (is= (eval* (parse "((lambda (a) (+ a 1)) 1)") buildin-env)
         [2.0 (cons {:a 1.0} buildin-env)])
    (is= (eval* (parse "((lambda (a) (+ a a)) 1)") buildin-env)
         [2.0 (cons {:a 1.0} buildin-env)])))
