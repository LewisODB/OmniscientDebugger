# FGET Help

An fget query is prolog-style, where this:

```prolog
port = call & callObject = <Thing_3> & arg0 > 33 & callMethodName = CMN
```

means 'find a call to a method, where the object is `<Thing_3>` and the first argument is an integer greater than 33. Then put the method name into the variable `CMN` and display it.'

There are two options on the **Trace** menu which will create interactive queries. One will create an FGET query which matches the currently selected trace line. Selecting that menu item will store the query as the previous FGET query, such that the next time you type `^F^F`, it will be the query that appears.

The other option will create a query which matches the current line of code. For example:

### Possible LHS Attributes

*   `(port p)`
*   `(sourceLine sl)`
*   `(sourceFile sf)`
*   `(thread thr)`
*   `(threadClass thrc)`
*   `(thisObject to)`
*   `(thisObjectClass toc)`
*   `(methodName mn)`
*   `(isMethodStatic ims)`
*   `(parameters params)`
*   `(callMethodName cmn)`
*   `(isCallMethodStatic icms)`
*   `(callObject co)`
*   `(callObjectClass coc)`
*   `(callArguments args)`
*   `(returnType rt)`
*   `(returnValue rv)`
*   `(name varName)`
*   `(type varType)`
*   `(newValue nv)`
*   `(oldValue ov)`
*   `(object o)`
*   `(lockType lt)`
*   `(objectClass oc)`
*   `(isIvarStatic iivs)`
*   `(blockedOnObject boo)`
*   `(blockedOnObjectClass booc)`
*   `(exception ex)`
*   `(exceptionClass exc)`
*   `(throwingMethodName thn)`
*   `(callArgumentValue0 a0 arg0 - arg9)`
*   `(parameterValue0 p0 - p9)`
*   `(objects os)`
*   `(var0 v0)`
*   `(var1 v1)`
*   `(vars vs)`
*   `(index i)`
*   `(array a)`
*   `(arrayClass ac)`
*   `(stackFrames sf)`
*   `(printString ps)`

### Possible RHS Constants

`catch`, `return`, `enter`, `(call c)`, `(exit x)`, `lock`, `(chgLocalVar clv)`, `(chgInstanceVar civ)`, `chgArray`, `(chgThreadState cts)`, `notdefined`, `null`, `false`, `true`, `boolean`, `byte`, `char`, `short`, `int`, `long`, `float`, `double`, `String`, `gettingLock`, `gotLock`, `releasingLock`, `startingWait`, `endingWait`, `startingJoin`, `endingJoin`

-- Bil Lewis (Bil.Lewis@LambdaCS.com)
