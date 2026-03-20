#!/bin/bash 
var=(var1 var2 var3)
echo ${var[@]} #print all items value
echo ${var[0]} #print index 0 value
echo ${#var[@]} #print the length of this array
echo ${var[@]:1:2} # print two value index 1  index 2
echo ${!var[@]} #print all items index
declare -A associativearray
associativearray=([name]="Hejiaming" [age]=23 [SID]="164139240393")
echo ${associativearray[name]}
echo ${associativearray[age]}
echo ${associativearray[SID]}

