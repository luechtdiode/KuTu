(window.webpackJsonp=window.webpackJsonp||[]).push([[0],{B5Ai:function(n,t,e){"use strict";e.d(t,"b",function(){return i}),e.d(t,"a",function(){return o}),e.d(t,"c",function(){return u});var r=function(n,t){return(r=Object.setPrototypeOf||{__proto__:[]}instanceof Array&&function(n,t){n.__proto__=t}||function(n,t){for(var e in t)t.hasOwnProperty(e)&&(n[e]=t[e])})(n,t)};function i(n,t){function e(){this.constructor=n}r(n,t),n.prototype=null===t?Object.create(t):(e.prototype=t.prototype,new e)}function o(n,t,e,r){return new(e||(e=Promise))(function(i,o){function u(n){try{a(r.next(n))}catch(t){o(t)}}function l(n){try{a(r.throw(n))}catch(t){o(t)}}function a(n){n.done?i(n.value):new e(function(t){t(n.value)}).then(u,l)}a((r=r.apply(n,t||[])).next())})}function u(n,t){var e,r,i,o,u={label:0,sent:function(){if(1&i[0])throw i[1];return i[1]},trys:[],ops:[]};return o={next:l(0),throw:l(1),return:l(2)},"function"==typeof Symbol&&(o[Symbol.iterator]=function(){return this}),o;function l(o){return function(l){return function(o){if(e)throw new TypeError("Generator is already executing.");for(;u;)try{if(e=1,r&&(i=2&o[0]?r.return:o[0]?r.throw||((i=r.return)&&i.call(r),0):r.next)&&!(i=i.call(r,o[1])).done)return i;switch(r=0,i&&(o=[2&o[0],i.value]),o[0]){case 0:case 1:i=o;break;case 4:return u.label++,{value:o[1],done:!1};case 5:u.label++,r=o[1],o=[0];continue;case 7:o=u.ops.pop(),u.trys.pop();continue;default:if(!(i=(i=u.trys).length>0&&i[i.length-1])&&(6===o[0]||2===o[0])){u=0;continue}if(3===o[0]&&(!i||o[1]>i[0]&&o[1]<i[3])){u.label=o[1];break}if(6===o[0]&&u.label<i[1]){u.label=i[1],i=o;break}if(i&&u.label<i[2]){u.label=i[2],u.ops.push(o);break}i[2]&&u.ops.pop(),u.trys.pop();continue}o=t.call(n,u)}catch(l){o=[6,l],r=0}finally{e=i=0}if(5&o[0])throw o[1];return{value:o[0]?o[1]:void 0,done:!0}}([o,l])}}}},Bs4g:function(n,t,e){"use strict";e.d(t,"a",function(){return r});var r=function(n,t){return function(n){return i(n)}(n).includes(t)},i=function(n){n.Ionic=n.Ionic||{};var t=n.Ionic.platforms;return null==t&&(t=n.Ionic.platforms=o(n)).forEach(function(t){return n.document.documentElement.classList.add("plt-"+t)}),t},o=function(n){return Object.keys(h).filter(function(t){return h[t](n)})},u=function(n){return f(n,/iPad/i)},l=function(n){return f(n,/android|sink/i)},a=function(n){return p(n,"(any-pointer:coarse)")},c=function(n){return s(n)||d(n)},s=function(n){return!!(n.cordova||n.phonegap||n.PhoneGap)},d=function(n){var t=n.Capacitor;return!(!t||!t.isNative)},f=function(n,t){return!(!n.navigator||!n.navigator.userAgent)&&t.test(n.navigator.userAgent)},p=function(n,t){return!!n.matchMedia&&n.matchMedia(t).matches},h={ipad:u,iphone:function(n){return f(n,/iPhone/i)},ios:function(n){return f(n,/iPad|iPhone|iPod/i)},android:l,phablet:function(n){var t=n.innerWidth,e=n.innerHeight,r=Math.min(t,e),i=Math.max(t,e);return r>390&&r<520&&i>620&&i<800},tablet:function(n){var t=n.innerWidth,e=n.innerHeight,r=Math.min(t,e),i=Math.max(t,e);return u(n)||function(n){return l(n)&&!f(n,/mobile/i)}(n)||r>460&&r<820&&i>780&&i<1400},cordova:s,capacitor:d,electron:function(n){return f(n,/electron/)},pwa:function(n){return!!n.matchMedia&&(n.matchMedia("(display-mode: standalone)").matches||n.navigator.standalone)},mobile:a,mobileweb:function(n){return a(n)&&!c(n)},desktop:function(n){return!a(n)},hybrid:c}},DQ87:function(n,t,e){"use strict";e.d(t,"a",function(){return h}),e.d(t,"b",function(){return p}),e.d(t,"c",function(){return a}),e.d(t,"d",function(){return c}),e.d(t,"e",function(){return f}),e.d(t,"f",function(){return o}),e.d(t,"g",function(){return u}),e.d(t,"h",function(){return l});var r=e("B5Ai"),i=0;function o(n,t){var e=n.ownerDocument;(function(n){0===i&&(i=1,n.addEventListener("focusin",function(t){var e=l(n);if(e&&e.backdropDismiss&&!function(n,t){for(;t;){if(t===n)return!0;t=t.parentElement}return!1}(e,t.target)){var r=e.querySelector("input,button");r&&r.focus()}}),n.addEventListener("ionBackButton",function(t){var e=l(n);e&&e.backdropDismiss&&t.detail.register(100,function(){return e.dismiss(void 0,h)})}),n.addEventListener("keyup",function(t){if("Escape"===t.key){var e=l(n);e&&e.backdropDismiss&&e.dismiss(void 0,h)}}))})(e),Object.assign(n,t),n.classList.add("overlay-hidden");var r=i++;return n.overlayIndex=r,n.hasAttribute("id")||(n.id="ion-overlay-"+r),s(e).appendChild(n),n.componentOnReady()}function u(n,t,e,r,i){var o=l(n,r,i);return o?o.dismiss(t,e):Promise.reject("overlay does not exist")}function l(n,t,e){var r=function(n,t){var e=Array.from(s(n).children).filter(function(n){return n.overlayIndex>0});return void 0===t?e:(t=t.toUpperCase(),e.filter(function(n){return n.tagName===t}))}(n,t);return void 0===e?r[r.length-1]:r.find(function(n){return n.id===e})}function a(n,t,e,i,o){return r.a(this,void 0,void 0,function(){var u;return r.c(this,function(r){switch(r.label){case 0:return n.presented?[2]:(n.presented=!0,n.willPresent.emit(),u=n.enterAnimation?n.enterAnimation:n.config.get(t,"ios"===n.mode?e:i),[4,d(n,u,n.el,o)]);case 1:return r.sent()&&n.didPresent.emit(),[2]}})})}function c(n,t,e,i,o,u,l){return r.a(this,void 0,void 0,function(){var a,c;return r.c(this,function(r){switch(r.label){case 0:if(!n.presented)return[2,!1];n.presented=!1,r.label=1;case 1:return r.trys.push([1,3,,4]),n.willDismiss.emit({data:t,role:e}),a=n.leaveAnimation?n.leaveAnimation:n.config.get(i,"ios"===n.mode?o:u),[4,d(n,a,n.el,l)];case 2:return r.sent(),n.didDismiss.emit({data:t,role:e}),[3,4];case 3:return c=r.sent(),console.error(c),[3,4];case 4:return n.el.remove(),[2,!0]}})})}function s(n){return n.querySelector("ion-app")||n.body}function d(n,t,i,o){return r.a(this,void 0,void 0,function(){var u,l,a,c;return r.c(this,function(r){switch(r.label){case 0:return n.animation?(n.animation.destroy(),n.animation=void 0,[2,!1]):(i.classList.remove("overlay-hidden"),u=i.shadowRoot||n.el,a=n,[4,e.e(2).then(e.bind(null,"ohUv")).then(function(n){return n.create(t,u,o)})]);case 1:return l=a.animation=r.sent(),n.animation=l,n.animated&&n.config.getBoolean("animated",!0)||l.duration(0),n.keyboardClose&&l.beforeAddWrite(function(){var n=i.ownerDocument.activeElement;n&&n.matches("input, ion-input, ion-textarea")&&n.blur()}),[4,l.playAsync()];case 2:return r.sent(),c=l.hasCompleted,l.destroy(),n.animation=void 0,[2,c]}})})}function f(n,t){var e,r=new Promise(function(n){return e=n});return function(n,t,e){var r=function(i){n.removeEventListener(t,r),e(i)};n.addEventListener(t,r)}(n,t,function(n){e(n.detail)}),r}function p(n){return"cancel"===n||n===h}var h="backdrop"},Gi3i:function(n,t,e){"use strict";e.d(t,"a",function(){return u});var r=e("mrSG"),i=e("FFOo"),o=e("T1DM");function u(n,t){return void 0===t&&(t=o.a),function(e){return e.lift(new l(n,t))}}var l=function(){function n(n,t){this.dueTime=n,this.scheduler=t}return n.prototype.call=function(n,t){return t.subscribe(new a(n,this.dueTime,this.scheduler))},n}(),a=function(n){function t(t,e,r){var i=n.call(this,t)||this;return i.dueTime=e,i.scheduler=r,i.debouncedSubscription=null,i.lastValue=null,i.hasValue=!1,i}return r.d(t,n),t.prototype._next=function(n){this.clearDebounce(),this.lastValue=n,this.hasValue=!0,this.add(this.debouncedSubscription=this.scheduler.schedule(c,this.dueTime,this))},t.prototype._complete=function(){this.debouncedNext(),this.destination.complete()},t.prototype.debouncedNext=function(){if(this.clearDebounce(),this.hasValue){var n=this.lastValue;this.lastValue=null,this.hasValue=!1,this.destination.next(n)}},t.prototype.clearDebounce=function(){var n=this.debouncedSubscription;null!==n&&(this.remove(n),n.unsubscribe(),this.debouncedSubscription=null)},t}(i.a);function c(n){n.debouncedNext()}},H2Ii:function(n,t,e){"use strict";e.d(t,"a",function(){return r});var r=function(){function n(){}return n.prototype.ngOnInit=function(){},n}()},JvIM:function(n,t,e){"use strict";function r(n){"requestIdleCallback"in window?window.requestIdleCallback(n):setTimeout(n,32)}function i(n){return!!n.shadowRoot&&!!n.attachShadow}function o(n){var t=n.closest("ion-item");return t?t.querySelector("ion-label"):null}function u(n,t,e,r,o){if(n||i(t)){var u=t.querySelector("input.aux-input");u||((u=t.ownerDocument.createElement("input")).type="hidden",u.classList.add("aux-input"),t.appendChild(u)),u.disabled=o,u.name=e,u.value=r||""}}function l(n,t,e){return Math.max(n,Math.min(t,e))}function a(n){return n.timeStamp||Date.now()}function c(n){if(n){var t=n.changedTouches;if(t&&t.length>0){var e=t[0];return{x:e.clientX,y:e.clientY}}if(void 0!==n.pageX)return{x:n.pageX,y:n.pageY}}return{x:0,y:0}}function s(n,t){var e="rtl"===n.document.dir;switch(t){case"start":return e;case"end":return!e;default:throw new Error('"'+t+'" is not a valid value for [side]. Use "start" or "end" instead.')}}function d(n,t){var e=n._original||n;return{_original:n,emit:f(e.emit.bind(e),t)}}function f(n,t){var e;return void 0===t&&(t=0),function(){for(var r=[],i=0;i<arguments.length;i++)r[i]=arguments[i];clearTimeout(e),e=setTimeout.apply(void 0,[n,t].concat(r))}}e.d(t,"a",function(){return r}),e.d(t,"b",function(){return a}),e.d(t,"c",function(){return i}),e.d(t,"d",function(){return o}),e.d(t,"e",function(){return u}),e.d(t,"f",function(){return d}),e.d(t,"g",function(){return s}),e.d(t,"h",function(){return l}),e.d(t,"i",function(){return f}),e.d(t,"j",function(){return c})},R5Yi:function(n,t,e){"use strict";e.r(t),e.d(t,"createGesture",function(){return f}),e.d(t,"GESTURE_CONTROLLER",function(){return a});var r,i=function(){function n(n){this.doc=n,this.gestureId=0,this.requestedStart=new Map,this.disabledGestures=new Map,this.disabledScroll=new Set}return n.prototype.createGesture=function(n){return new o(this,this.newID(),n.name,n.priority||0,!!n.disableScroll)},n.prototype.createBlocker=function(n){return void 0===n&&(n={}),new u(this,this.newID(),n.disable,!!n.disableScroll)},n.prototype.start=function(n,t,e){return this.canStart(n)?(this.requestedStart.set(t,e),!0):(this.requestedStart.delete(t),!1)},n.prototype.capture=function(n,t,e){if(!this.start(n,t,e))return!1;var r=this.requestedStart,i=-1e4;if(r.forEach(function(n){i=Math.max(i,n)}),i===e){this.capturedId=t,r.clear();var o=new CustomEvent("ionGestureCaptured",{detail:{gestureName:n}});return this.doc.dispatchEvent(o),!0}return r.delete(t),!1},n.prototype.release=function(n){this.requestedStart.delete(n),this.capturedId===n&&(this.capturedId=void 0)},n.prototype.disableGesture=function(n,t){var e=this.disabledGestures.get(n);void 0===e&&(e=new Set,this.disabledGestures.set(n,e)),e.add(t)},n.prototype.enableGesture=function(n,t){var e=this.disabledGestures.get(n);void 0!==e&&e.delete(t)},n.prototype.disableScroll=function(n){this.disabledScroll.add(n),1===this.disabledScroll.size&&this.doc.body.classList.add(l)},n.prototype.enableScroll=function(n){this.disabledScroll.delete(n),0===this.disabledScroll.size&&this.doc.body.classList.remove(l)},n.prototype.canStart=function(n){return void 0===this.capturedId&&!this.isDisabled(n)},n.prototype.isCaptured=function(){return void 0!==this.capturedId},n.prototype.isScrollDisabled=function(){return this.disabledScroll.size>0},n.prototype.isDisabled=function(n){var t=this.disabledGestures.get(n);return!!(t&&t.size>0)},n.prototype.newID=function(){return this.gestureId++,this.gestureId},n}(),o=function(){function n(n,t,e,r,i){this.id=t,this.name=e,this.disableScroll=i,this.priority=1e6*r+t,this.ctrl=n}return n.prototype.canStart=function(){return!!this.ctrl&&this.ctrl.canStart(this.name)},n.prototype.start=function(){return!!this.ctrl&&this.ctrl.start(this.name,this.id,this.priority)},n.prototype.capture=function(){if(!this.ctrl)return!1;var n=this.ctrl.capture(this.name,this.id,this.priority);return n&&this.disableScroll&&this.ctrl.disableScroll(this.id),n},n.prototype.release=function(){this.ctrl&&(this.ctrl.release(this.id),this.disableScroll&&this.ctrl.enableScroll(this.id))},n.prototype.destroy=function(){this.release(),this.ctrl=void 0},n}(),u=function(){function n(n,t,e,r){this.id=t,this.disable=e,this.disableScroll=r,this.ctrl=n}return n.prototype.block=function(){if(this.ctrl){if(this.disable)for(var n=0,t=this.disable;n<t.length;n++)this.ctrl.disableGesture(t[n],this.id);this.disableScroll&&this.ctrl.disableScroll(this.id)}},n.prototype.unblock=function(){if(this.ctrl){if(this.disable)for(var n=0,t=this.disable;n<t.length;n++)this.ctrl.enableGesture(t[n],this.id);this.disableScroll&&this.ctrl.enableScroll(this.id)}},n.prototype.destroy=function(){this.unblock(),this.ctrl=void 0},n}(),l="backdrop-no-scroll",a=new i(document);function c(n,t,e,i){var o,u,l=function(n){if(void 0===r)try{var t=Object.defineProperty({},"passive",{get:function(){r=!0}});n.addEventListener("optsTest",function(){},t)}catch(n){r=!1}return!!r}(n)?{capture:!!i.capture,passive:!!i.passive}:!!i.capture;return n.__zone_symbol__addEventListener?(o="__zone_symbol__addEventListener",u="__zone_symbol__removeEventListener"):(o="addEventListener",u="removeEventListener"),n[o](t,e,l),function(){n[u](t,e,l)}}var s=2e3;function d(n){return n instanceof Document?n:n.ownerDocument}function f(n){var t=Object.assign({disableScroll:!1,direction:"x",gesturePriority:0,passive:!0,maxAngle:40,threshold:10},n),e=t.canStart,r=t.onWillStart,i=t.onStart,o=t.onEnd,u=t.notCaptured,l=t.onMove,f=t.threshold,b=t.queue,m={type:"pan",startX:0,startY:0,startTimeStamp:0,currentX:0,currentY:0,velocityX:0,velocityY:0,deltaX:0,deltaY:0,timeStamp:0,event:void 0,data:void 0},y=function(n,t,e,r,i){var o,u,l,a,f,p,h,v=0;function b(r){v=Date.now()+s,t(r)&&(!u&&e&&(u=c(n,"touchmove",e,i)),l||(l=c(n,"touchend",y,i)),a||(a=c(n,"touchcancel",y,i)))}function m(r){v>Date.now()||t(r)&&(!p&&e&&(p=c(d(n),"mousemove",e,i)),h||(h=c(d(n),"mouseup",g,i)))}function y(n){w(),r&&r(n)}function g(n){S(),r&&r(n)}function w(){u&&u(),l&&l(),a&&a(),u=l=a=void 0}function S(){p&&p(),h&&h(),p=h=void 0}function E(){w(),S()}function k(t){t?(o&&o(),f&&f(),o=f=void 0,E()):(o||(o=c(n,"touchstart",b,i)),f||(f=c(n,"mousedown",m,i)))}return{setDisabled:k,stop:E,destroy:function(){k(!0),r=e=t=void 0}}}(t.el,function(n){var t=v(n);return!(E||!k)&&(h(n,m),m.startX=m.currentX,m.startY=m.currentY,m.startTimeStamp=m.timeStamp=t,m.velocityX=m.velocityY=m.deltaX=m.deltaY=0,m.event=n,(!e||!1!==e(m))&&(w.release(),!!w.start()&&(E=!0,0===f?x():(g.start(m.startX,m.startY),!0))))},function(n){S?!O&&k&&(O=!0,p(m,n),b.write(D)):(p(m,n),g.detect(m.currentX,m.currentY)&&(g.isGesture()&&x()||(A(),y.stop(),u&&u(m))))},I,{capture:!1}),g=function(n,t,e){var r=e*(Math.PI/180),i="x"===n,o=Math.cos(r),u=t*t,l=0,a=0,c=!1,s=0;return{start:function(n,t){l=n,a=t,s=0,c=!0},detect:function(n,t){if(!c)return!1;var e=n-l,r=t-a,d=e*e+r*r;if(d<u)return!1;var f=Math.sqrt(d),p=(i?e:r)/f;return s=p>o?1:p<-o?-1:0,c=!1,!0},isGesture:function(){return 0!==s},getDirection:function(){return s}}}(t.direction,t.threshold,t.maxAngle),w=a.createGesture({name:n.gestureName,priority:n.gesturePriority,disableScroll:n.disableScroll}),S=!1,E=!1,k=!0,O=!1;function D(){S&&(O=!1,l&&l(m))}function x(){return!(w&&!w.capture()||(S=!0,k=!1,m.startX=m.currentX,m.startY=m.currentY,m.startTimeStamp=m.timeStamp,r?r(m).then(L):L(),0))}function L(){i&&i(m),k=!0}function A(){S=!1,E=!1,O=!1,k=!0,w.release()}function I(n){var t=S,e=k;A(),e&&(p(m,n),t?o&&o(m):u&&u(m))}return{setDisabled:function(n){n&&S&&I(void 0),y.setDisabled(n)},destroy:function(){w.destroy(),y.destroy()}}}function p(n,t){if(t){var e=n.currentX,r=n.currentY,i=n.timeStamp;h(t,n);var o=n.currentX,u=n.currentY,l=(n.timeStamp=v(t))-i;if(l>0&&l<100){var a=(u-r)/l;n.velocityX=(o-e)/l*.7+.3*n.velocityX,n.velocityY=.7*a+.3*n.velocityY}n.deltaX=o-n.startX,n.deltaY=u-n.startY,n.event=t}}function h(n,t){var e=0,r=0;if(n){var i=n.changedTouches;if(i&&i.length>0){var o=i[0];e=o.clientX,r=o.clientY}else void 0!==n.pageX&&(e=n.pageX,r=n.pageY)}t.currentX=e,t.currentY=r}function v(n){return n.timeStamp||Date.now()}},WmWN:function(n,t,e){"use strict";e.d(t,"a",function(){return r});var r=function(n){try{if("string"!=typeof n||""===n)return n;var t=document.createDocumentFragment(),e=document.createElement("div");t.appendChild(e),e.innerHTML=n,l.forEach(function(n){for(var e=t.querySelectorAll(n),r=e.length-1;r>=0;r--){var u=e[r];u.parentNode?u.parentNode.removeChild(u):t.removeChild(u);for(var l=o(u),a=0;a<l.length;a++)i(l[a])}});for(var r=o(t),u=0;u<r.length;u++)i(r[u]);var a=document.createElement("div");a.appendChild(t);var c=a.querySelector("div");return null!==c?c.innerHTML:a.innerHTML}catch(n){return console.error(n),""}},i=function(n){if(!n.nodeType||1===n.nodeType){for(var t=n.attributes.length-1;t>=0;t--){var e=n.attributes[t],r=e.name;if(u.includes(r.toLowerCase())){var l=e.value;null!=l&&l.toLowerCase().includes("javascript:")&&n.removeAttribute(r)}else n.removeAttribute(r)}var a=o(n);for(t=0;t<a.length;t++)i(a[t])}},o=function(n){return null!=n.children?n.children:n.childNodes},u=["class","id","href","src","name","slot"],l=["script","style","iframe","meta","link","object","embed"]},Zr1d:function(n,t,e){"use strict";e.d(t,"a",function(){return o});var r=e("mrSG"),i=e("a3Cf"),o=function(n){function t(){return null!==n&&n.apply(this,arguments)||this}return Object(r.d)(t,n),t.prototype.hideFormAccessoryBar=function(n){return Object(i.cordova)(this,"hideFormAccessoryBar",{sync:!0},arguments)},t.prototype.hide=function(){return Object(i.cordova)(this,"hide",{sync:!0,platforms:["iOS","Android"]},arguments)},t.prototype.show=function(){return Object(i.cordova)(this,"show",{sync:!0,platforms:["Android"]},arguments)},t.prototype.setResizeMode=function(n){return Object(i.cordova)(this,"setResizeMode",{sync:!0,platforms:["iOS"]},arguments)},t.prototype.onKeyboardShow=function(){return Object(i.cordova)(this,"onKeyboardShow",{eventObservable:!0,event:"native.keyboardshow",platforms:["iOS","Android"]},arguments)},t.prototype.onKeyboardWillShow=function(){return Object(i.cordova)(this,"onKeyboardWillShow",{eventObservable:!0,event:"keyboardWillShow",platforms:["iOS","Android"]},arguments)},t.prototype.onKeyboardHide=function(){return Object(i.cordova)(this,"onKeyboardHide",{eventObservable:!0,event:"native.keyboardhide",platforms:["iOS","Android"]},arguments)},t.prototype.onKeyboardWillHide=function(){return Object(i.cordova)(this,"onKeyboardWillHide",{eventObservable:!0,event:"keyboardWillHide",platforms:["iOS","Android"]},arguments)},Object.defineProperty(t.prototype,"isVisible",{get:function(){return Object(i.cordovaPropertyGet)(this,"isVisible")},set:function(n){Object(i.cordovaPropertySet)(this,"isVisible",n)},enumerable:!0,configurable:!0}),t.pluginName="Keyboard",t.plugin="cordova-plugin-ionic-keyboard",t.pluginRef="window.Keyboard",t.repo="https://github.com/ionic-team/cordova-plugin-ionic-keyboard",t.platforms=["Android","iOS"],t}(i.IonicNativePlugin)},awvO:function(n,t,e){"use strict";e.d(t,"a",function(){return o}),e.d(t,"b",function(){return r}),e.d(t,"c",function(){return i}),e.d(t,"d",function(){return u}),e.d(t,"e",function(){return l});var r="ionViewWillEnter",i="ionViewDidEnter",o="ionViewWillLeave",u="ionViewDidLeave",l="ionViewWillUnload"},bDjW:function(n,t,e){"use strict";e.d(t,"a",function(){return r});var r=function(){return function(){}}()},cDAM:function(n,t,e){"use strict";e.d(t,"a",function(){return g}),e.d(t,"b",function(){return m}),e.d(t,"c",function(){return l}),e.d(t,"d",function(){return w});var r=e("B5Ai"),i=e("awvO"),o=function(){return e.e(60).then(e.bind(null,"rSHd"))},u=function(){return e.e(61).then(e.bind(null,"NJz6"))};function l(n){return new Promise(function(t,e){n.queue.write(function(){(function(n){var t=n.enteringEl,e=n.leavingEl;(function(n,t,e){void 0!==n&&(n.style.zIndex="back"===e?"99":"101"),void 0!==t&&(t.style.zIndex="100")})(t,e,n.direction),n.showGoBack?t.classList.add("can-go-back"):t.classList.remove("can-go-back"),w(t,!1),e&&w(e,!1)})(n),function(n){return r.a(this,void 0,void 0,function(){var t;return r.c(this,function(e){switch(e.label){case 0:return[4,c(n)];case 1:return[2,(t=e.sent())?s(t,n):d(n)]}})})}(n).then(function(e){e.animation&&e.animation.destroy(),a(n),t(e)},function(t){a(n),e(t)})})})}function a(n){var t=n.leavingEl;n.enteringEl.classList.remove("ion-page-invisible"),void 0!==t&&t.classList.remove("ion-page-invisible")}function c(n){return r.a(this,void 0,void 0,function(){var t;return r.c(this,function(e){switch(e.label){case 0:return n.leavingEl&&n.animated&&0!==n.duration?n.animationBuilder?[2,n.animationBuilder]:"ios"!==n.mode?[3,2]:[4,o()]:[2,void 0];case 1:return t=e.sent().iosTransitionAnimation,[3,4];case 2:return[4,u()];case 3:t=e.sent().mdTransitionAnimation,e.label=4;case 4:return[2,t]}})})}function s(n,t){return r.a(this,void 0,void 0,function(){var i;return r.c(this,function(r){switch(r.label){case 0:return[4,f(t,!0)];case 1:return r.sent(),[4,e.e(2).then(e.bind(null,"ohUv")).then(function(e){return e.create(n,t.baseEl,t)})];case 2:return i=r.sent(),v(t.enteringEl,t.leavingEl),[4,h(i,t)];case 3:return r.sent(),t.progressCallback&&t.progressCallback(void 0),i.hasCompleted&&b(t.enteringEl,t.leavingEl),[2,{hasCompleted:i.hasCompleted,animation:i}]}})})}function d(n){return r.a(this,void 0,void 0,function(){var t,e;return r.c(this,function(r){switch(r.label){case 0:return t=n.enteringEl,e=n.leavingEl,[4,f(n,!1)];case 1:return r.sent(),v(t,e),b(t,e),[2,{hasCompleted:!0}]}})})}function f(n,t){return r.a(this,void 0,void 0,function(){var e;return r.c(this,function(r){switch(r.label){case 0:return e=(void 0!==n.deepWait?n.deepWait:t)?[g(n.enteringEl),g(n.leavingEl)]:[y(n.enteringEl),y(n.leavingEl)],[4,Promise.all(e)];case 1:return r.sent(),[4,p(n.viewIsReady,n.enteringEl)];case 2:return r.sent(),[2]}})})}function p(n,t){return r.a(this,void 0,void 0,function(){return r.c(this,function(e){switch(e.label){case 0:return n?[4,n(t)]:[3,2];case 1:e.sent(),e.label=2;case 2:return[2]}})})}function h(n,t){var e=t.progressCallback,r=new Promise(function(t){return n.onFinish(t)});return e?(n.progressStart(),e(n)):n.play(),r}function v(n,t){m(t,i.a),m(n,i.b)}function b(n,t){m(n,i.c),m(t,i.d)}function m(n,t){if(n){var e=new CustomEvent(t,{bubbles:!1,cancelable:!1});n.dispatchEvent(e)}}function y(n){return n&&n.componentOnReady?n.componentOnReady():Promise.resolve()}function g(n){return r.a(this,void 0,void 0,function(){var t;return r.c(this,function(e){switch(e.label){case 0:return(t=n)?null==t.componentOnReady?[3,2]:[4,t.componentOnReady()]:[3,4];case 1:if(null!=e.sent())return[2];e.label=2;case 2:return[4,Promise.all(Array.from(t.children).map(g))];case 3:e.sent(),e.label=4;case 4:return[2]}})})}function w(n,t){t?(n.setAttribute("aria-hidden","true"),n.classList.add("ion-page-hidden")):(n.hidden=!1,n.removeAttribute("aria-hidden"),n.classList.remove("ion-page-hidden"))}},d6Vy:function(n,t,e){"use strict";e.d(t,"a",function(){return u}),e.d(t,"b",function(){return a}),e.d(t,"c",function(){return o}),e.d(t,"d",function(){return i});var r=e("B5Ai");function i(n,t){return null!==t.closest(n)}function o(n){var t;return"string"==typeof n&&n.length>0?((t={"ion-color":!0})["ion-color-"+n]=!0,t):void 0}function u(n){var t={};return function(n){return void 0!==n?(Array.isArray(n)?n:n.split(" ")).filter(function(n){return null!=n}).map(function(n){return n.trim()}).filter(function(n){return""!==n}):[]}(n).forEach(function(n){return t[n]=!0}),t}var l=/^[a-z][a-z0-9+\-.]*:/;function a(n,t,e,i){return r.a(this,void 0,void 0,function(){var o;return r.c(this,function(r){switch(r.label){case 0:return null==t||"#"===t[0]||l.test(t)?[3,2]:(o=n.document.querySelector("ion-router"))?(null!=e&&e.preventDefault(),[4,o.componentOnReady()]):[3,2];case 1:return r.sent(),[2,o.push(t,i)];case 2:return[2,!1]}})})}},jT1R:function(n,t,e){"use strict";e.d(t,"a",function(){return i}),e.d(t,"b",function(){return o});var r=e("B5Ai");function i(n,t,e,i,o){return r.a(this,void 0,void 0,function(){var u;return r.c(this,function(r){switch(r.label){case 0:if(n)return[2,n.attachViewToDom(t,e,o,i)];if("string"!=typeof e&&!(e instanceof HTMLElement))throw new Error("framework delegate is missing");return u="string"==typeof e?t.ownerDocument&&t.ownerDocument.createElement(e):e,i&&i.forEach(function(n){return u.classList.add(n)}),o&&Object.assign(u,o),t.appendChild(u),u.componentOnReady?[4,u.componentOnReady()]:[3,2];case 1:r.sent(),r.label=2;case 2:return[2,u]}})})}function o(n,t){if(t){if(n)return n.removeViewFromDom(t.parentElement,t);t.remove()}return Promise.resolve()}},ppBR:function(n,t){},wj5n:function(n,t,e){"use strict";var r=e("CcnG"),i=e("oBZk"),o=e("ZZ/e"),u=e("Ip0R");e("H2Ii"),e.d(t,"a",function(){return l}),e.d(t,"b",function(){return d});var l=r.nb({encapsulation:0,styles:[["ion-card[_ngcontent-%COMP%]{margin:auto;position:relative;color:var(--ion-card-color);background-color:var(--ion-card-background-color);border-radius:4px;box-shadow:0 3px 1px -2px var(--ion-card-shadow-color1),0 2px 2px 0 var(--ion-card-shadow-color2),0 1px 5px 0 var(--ion-card-shadow-color3)}"]],data:{}});function a(n){return r.Hb(0,[(n()(),r.pb(0,0,null,null,10,"ion-note",[["item-end",""],["item-text-wrap",""]],null,null,null,i.bb,i.w)),r.ob(1,49152,null,0,o.W,[r.h,r.k],null,null),(n()(),r.Fb(2,0,[" D: ","\xa0\xa0 "])),r.Bb(3,2),(n()(),r.pb(4,0,null,0,0,"br",[],null,null,null,null,null)),(n()(),r.Fb(5,0,["E: ","\xa0\xa0 "])),r.Bb(6,2),(n()(),r.pb(7,0,null,0,0,"br",[],null,null,null,null,null)),(n()(),r.pb(8,0,null,0,2,"span",[["style","font-size: 18pt;font-weight: bold"]],null,null,null,null,null)),(n()(),r.Fb(9,null,["","\xa0\xa0"])),r.Bb(10,2)],null,function(n,t){var e=t.component,i=r.Gb(t,2,0,n(t,3,0,r.yb(t.parent,0),e.item.wertung.noteD,"2.3-3"));n(t,2,0,i);var o=r.Gb(t,5,0,n(t,6,0,r.yb(t.parent,0),e.item.wertung.noteE,"2.3-3"));n(t,5,0,o);var u=r.Gb(t,9,0,n(t,10,0,r.yb(t.parent,0),e.item.wertung.endnote,"2.3-3"));n(t,9,0,u)})}function c(n){return r.Hb(0,[(n()(),r.pb(0,0,null,null,8,"ion-note",[["item-end",""],["item-text-wrap",""]],null,null,null,i.bb,i.w)),r.ob(1,49152,null,0,o.W,[r.h,r.k],null,null),(n()(),r.pb(2,0,null,0,0,"br",[],null,null,null,null,null)),(n()(),r.Fb(3,0,["E: ","\xa0\xa0 "])),r.Bb(4,2),(n()(),r.pb(5,0,null,0,0,"br",[],null,null,null,null,null)),(n()(),r.pb(6,0,null,0,2,"span",[["style","font-size: 18pt;font-weight: bold"]],null,null,null,null,null)),(n()(),r.Fb(7,null,["","\xa0\xa0"])),r.Bb(8,2)],null,function(n,t){var e=t.component,i=r.Gb(t,3,0,n(t,4,0,r.yb(t.parent,0),e.item.wertung.noteE,"2.3-3"));n(t,3,0,i);var o=r.Gb(t,7,0,n(t,8,0,r.yb(t.parent,0),e.item.wertung.endnote,"2.3-3"));n(t,7,0,o)})}function s(n){return r.Hb(0,[(n()(),r.pb(0,0,null,null,4,"ion-note",[["item-end",""],["item-text-wrap",""]],null,null,null,i.bb,i.w)),r.ob(1,49152,null,0,o.W,[r.h,r.k],null,null),(n()(),r.pb(2,0,null,0,2,"span",[["style","font-size: 18pt;font-weight: bold"]],null,null,null,null,null)),(n()(),r.Fb(3,null,["","\xa0\xa0"])),r.Bb(4,2)],null,function(n,t){var e=t.component,i=r.Gb(t,3,0,n(t,4,0,r.yb(t.parent,0),e.item.wertung.endnote,"2.3-3"));n(t,3,0,i)})}function d(n){return r.Hb(0,[r.zb(0,u.f,[r.u]),(n()(),r.pb(1,0,null,null,27,"ion-card",[],null,null,null,i.K,i.f)),r.ob(2,49152,null,0,o.l,[r.h,r.k],null,null),(n()(),r.pb(3,0,null,0,25,"ion-grid",[],null,null,null,i.O,i.j)),r.ob(4,49152,null,0,o.z,[r.h,r.k],null,null),(n()(),r.pb(5,0,null,0,6,"ion-row",[],null,null,null,i.cb,i.x)),r.ob(6,49152,null,0,o.hb,[r.h,r.k],null,null),(n()(),r.pb(7,0,null,0,4,"ion-col",[["align-self-top",""]],null,null,null,i.L,i.g)),r.ob(8,49152,null,0,o.s,[r.h,r.k],null,null),(n()(),r.pb(9,0,null,0,1,"div",[["style","font-size: 20pt;"]],null,null,null,null,null)),(n()(),r.Fb(10,null,["",""])),(n()(),r.Fb(-1,0,["\xa0\xa0 "])),(n()(),r.pb(12,0,null,0,16,"ion-row",[],null,null,null,i.cb,i.x)),r.ob(13,49152,null,0,o.hb,[r.h,r.k],null,null),(n()(),r.pb(14,0,null,0,6,"ion-col",[["align-self-top",""]],null,null,null,i.L,i.g)),r.ob(15,49152,null,0,o.s,[r.h,r.k],null,null),(n()(),r.pb(16,0,null,0,1,"div",[["style","font-size: 16pt;"]],null,null,null,null,null)),(n()(),r.Fb(17,null,["",""])),(n()(),r.Fb(-1,0,["\xa0\xa0 "])),(n()(),r.pb(19,0,null,0,0,"br",[],null,null,null,null,null)),(n()(),r.Fb(20,0,[" ","\xa0\xa0 "])),(n()(),r.pb(21,0,null,0,7,"ion-col",[["align-self-end",""],["col-auto",""]],null,null,null,i.L,i.g)),r.ob(22,49152,null,0,o.s,[r.h,r.k],null,null),(n()(),r.gb(16777216,null,0,1,null,a)),r.ob(24,16384,null,0,u.m,[r.O,r.L],{ngIf:[0,"ngIf"]},null),(n()(),r.gb(16777216,null,0,1,null,c)),r.ob(26,16384,null,0,u.m,[r.O,r.L],{ngIf:[0,"ngIf"]},null),(n()(),r.gb(16777216,null,0,1,null,s)),r.ob(28,16384,null,0,u.m,[r.O,r.L],{ngIf:[0,"ngIf"]},null)],function(n,t){var e=t.component;n(t,24,0,void 0!==e.item.wertung.noteD),n(t,26,0,void 0===e.item.wertung.noteD&&e.item.wertung.noteE!==e.item.wertung.endnote),n(t,28,0,void 0===e.item.wertung.noteD&&e.item.wertung.noteE===e.item.wertung.endnote)},function(n,t){var e=t.component;n(t,10,0,e.title),n(t,17,0,e.item.vorname+" "+e.item.name),n(t,20,0,e.item.verein)})}},ySCp:function(n,t,e){"use strict";function r(){var n=window.TapticEngine;n&&n.selection()}function i(){var n=window.TapticEngine;n&&n.gestureSelectionStart()}function o(){var n=window.TapticEngine;n&&n.gestureSelectionChanged()}function u(){var n=window.TapticEngine;n&&n.gestureSelectionEnd()}e.d(t,"a",function(){return o}),e.d(t,"b",function(){return i}),e.d(t,"c",function(){return u}),e.d(t,"d",function(){return r})}}]);