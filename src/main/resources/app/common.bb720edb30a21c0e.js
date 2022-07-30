"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[8592],{4302:(E,v,u)=>{u.d(v,{N1:()=>r});var e=u(655),m=u(5419),g=u(5675),r=function(n){function t(){return null!==n&&n.apply(this,arguments)||this}return(0,e.ZT)(t,n),t.prototype.hideFormAccessoryBar=function(o){return(0,m.DM)(this,"hideFormAccessoryBar",{sync:!0,platforms:["iOS"]},arguments)},t.prototype.hide=function(){return(0,m.DM)(this,"hide",{sync:!0,platforms:["iOS","Android"]},arguments)},t.prototype.show=function(){return(0,m.DM)(this,"show",{sync:!0,platforms:["Android"]},arguments)},t.prototype.setResizeMode=function(o){return(0,m.DM)(this,"setResizeMode",{sync:!0,platforms:["iOS"]},arguments)},t.prototype.setKeyboardStyle=function(o){return(0,m.DM)(this,"setKeyboardStyle",{sync:!0,platforms:["iOS"]},arguments)},t.prototype.disableScroll=function(o){return(0,m.DM)(this,"disableScroll",{sync:!0,platforms:["iOS"]},arguments)},t.prototype.onKeyboardShow=function(){return(0,m.DM)(this,"onKeyboardShow",{eventObservable:!0,event:"native.keyboardshow",platforms:["iOS","Android"]},arguments)},t.prototype.onKeyboardWillShow=function(){return(0,m.DM)(this,"onKeyboardWillShow",{eventObservable:!0,event:"keyboardWillShow",platforms:["iOS","Android"]},arguments)},t.prototype.onKeyboardDidShow=function(){return(0,m.DM)(this,"onKeyboardDidShow",{eventObservable:!0,event:"keyboardDidShow",platforms:["iOS","Android"]},arguments)},t.prototype.onKeyboardHide=function(){return(0,m.DM)(this,"onKeyboardHide",{eventObservable:!0,event:"native.keyboardhide",platforms:["iOS","Android"]},arguments)},t.prototype.onKeyboardWillHide=function(){return(0,m.DM)(this,"onKeyboardWillHide",{eventObservable:!0,event:"keyboardWillHide",platforms:["iOS","Android"]},arguments)},t.prototype.onKeyboardDidHide=function(){return(0,m.DM)(this,"onKeyboardDidHide",{eventObservable:!0,event:"keyboardDidHide",platforms:["iOS","Android"]},arguments)},Object.defineProperty(t.prototype,"isVisible",{get:function(){return(0,m.J2)(this,"isVisible")},set:function(o){(0,m.Iq)(this,"isVisible",o)},enumerable:!1,configurable:!0}),t.pluginName="Keyboard",t.plugin="cordova-plugin-ionic-keyboard",t.pluginRef="window.Keyboard",t.repo="https://github.com/ionic-team/cordova-plugin-ionic-keyboard",t.platforms=["Android","iOS"],t.\u0275fac=function(){var o;return function(h){return(o||(o=g.n5z(t)))(h||t)}}(),t.\u0275prov=g.Yz7({token:t,factory:function(o){return t.\u0275fac(o)}}),t}(m.KY)},4833:(E,v,u)=>{u.d(v,{c:()=>d});var e=u(7205),m=u(7683),g=u(3139);const d=(l,r)=>{let n,t;const o=(p,f,w)=>{if(typeof document>"u")return;const c=document.elementFromPoint(p,f);c&&r(c)?c!==n&&(h(),s(c,w)):h()},s=(p,f)=>{n=p,t||(t=n);const w=n;(0,e.c)(()=>w.classList.add("ion-activated")),f()},h=(p=!1)=>{if(!n)return;const f=n;(0,e.c)(()=>f.classList.remove("ion-activated")),p&&t!==n&&n.click(),n=void 0};return(0,g.createGesture)({el:l,gestureName:"buttonActiveDrag",threshold:0,onStart:p=>o(p.currentX,p.currentY,m.a),onMove:p=>o(p.currentX,p.currentY,m.b),onEnd:()=>{h(!0),(0,m.h)(),t=void 0}})}},8685:(E,v,u)=>{u.d(v,{g:()=>e});const e=(r,n,t,o,s)=>g(r[1],n[1],t[1],o[1],s).map(h=>m(r[0],n[0],t[0],o[0],h)),m=(r,n,t,o,s)=>s*(3*n*Math.pow(s-1,2)+s*(-3*t*s+3*t+o*s))-r*Math.pow(s-1,3),g=(r,n,t,o,s)=>l((o-=s)-3*(t-=s)+3*(n-=s)-(r-=s),3*t-6*n+3*r,3*n-3*r,r).filter(p=>p>=0&&p<=1),l=(r,n,t,o)=>{if(0===r)return((r,n,t)=>{const o=n*n-4*r*t;return o<0?[]:[(-n+Math.sqrt(o))/(2*r),(-n-Math.sqrt(o))/(2*r)]})(n,t,o);const s=(3*(t/=r)-(n/=r)*n)/3,h=(2*n*n*n-9*n*t+27*(o/=r))/27;if(0===s)return[Math.pow(-h,1/3)];if(0===h)return[Math.sqrt(-s),-Math.sqrt(-s)];const p=Math.pow(h/2,2)+Math.pow(s/3,3);if(0===p)return[Math.pow(h/2,.5)-n/3];if(p>0)return[Math.pow(-h/2+Math.sqrt(p),1/3)-Math.pow(h/2+Math.sqrt(p),1/3)-n/3];const f=Math.sqrt(Math.pow(-s/3,3)),w=Math.acos(-h/(2*Math.sqrt(Math.pow(-s/3,3)))),c=2*Math.pow(f,1/3);return[c*Math.cos(w/3)-n/3,c*Math.cos((w+2*Math.PI)/3)-n/3,c*Math.cos((w+4*Math.PI)/3)-n/3]}},5062:(E,v,u)=>{u.d(v,{i:()=>e});const e=m=>m&&""!==m.dir?"rtl"===m.dir.toLowerCase():"rtl"===document?.dir.toLowerCase()},1112:(E,v,u)=>{u.r(v),u.d(v,{startFocusVisible:()=>d});const e="ion-focused",g=["Tab","ArrowDown","Space","Escape"," ","Shift","Enter","ArrowLeft","ArrowRight","ArrowUp","Home","End"],d=l=>{let r=[],n=!0;const t=l?l.shadowRoot:document,o=l||document.body,s=y=>{r.forEach(i=>i.classList.remove(e)),y.forEach(i=>i.classList.add(e)),r=y},h=()=>{n=!1,s([])},p=y=>{n=g.includes(y.key),n||s([])},f=y=>{if(n&&y.composedPath){const i=y.composedPath().filter(_=>!!_.classList&&_.classList.contains("ion-focusable"));s(i)}},w=()=>{t.activeElement===o&&s([])};return t.addEventListener("keydown",p),t.addEventListener("focusin",f),t.addEventListener("focusout",w),t.addEventListener("touchstart",h),t.addEventListener("mousedown",h),{destroy:()=>{t.removeEventListener("keydown",p),t.removeEventListener("focusin",f),t.removeEventListener("focusout",w),t.removeEventListener("touchstart",h),t.removeEventListener("mousedown",h)},setFocus:s}}},1878:(E,v,u)=>{u.d(v,{C:()=>l,a:()=>g,d:()=>d});var e=u(5861),m=u(3756);const g=function(){var r=(0,e.Z)(function*(n,t,o,s,h,p){var f;if(n)return n.attachViewToDom(t,o,h,s);if(!(p||"string"==typeof o||o instanceof HTMLElement))throw new Error("framework delegate is missing");const w="string"==typeof o?null===(f=t.ownerDocument)||void 0===f?void 0:f.createElement(o):o;return s&&s.forEach(c=>w.classList.add(c)),h&&Object.assign(w,h),t.appendChild(w),yield new Promise(c=>(0,m.c)(w,c)),w});return function(t,o,s,h,p,f){return r.apply(this,arguments)}}(),d=(r,n)=>{if(n){if(r)return r.removeViewFromDom(n.parentElement,n);n.remove()}return Promise.resolve()},l=()=>{let r,n;return{attachViewToDom:function(){var s=(0,e.Z)(function*(h,p,f={},w=[]){var c,y;if(r=h,p){const _="string"==typeof p?null===(c=r.ownerDocument)||void 0===c?void 0:c.createElement(p):p;w.forEach(a=>_.classList.add(a)),Object.assign(_,f),r.appendChild(_),yield new Promise(a=>(0,m.c)(_,a))}else if(r.children.length>0){const _=null===(y=r.ownerDocument)||void 0===y?void 0:y.createElement("div");w.forEach(a=>_.classList.add(a)),_.append(...r.children),r.appendChild(_)}const i=document.querySelector("ion-app")||document.body;return n=document.createComment("ionic teleport"),r.parentNode.insertBefore(n,r),i.appendChild(r),r});return function(p,f){return s.apply(this,arguments)}}(),removeViewFromDom:()=>(r&&n&&(n.parentNode.insertBefore(r,n),n.remove()),Promise.resolve())}}},7683:(E,v,u)=>{u.d(v,{a:()=>g,b:()=>d,c:()=>m,d:()=>r,h:()=>l});const e={getEngine(){var n;const t=window;return t.TapticEngine||(null===(n=t.Capacitor)||void 0===n?void 0:n.isPluginAvailable("Haptics"))&&t.Capacitor.Plugins.Haptics},available(){return!!this.getEngine()},isCordova:()=>!!window.TapticEngine,isCapacitor:()=>!!window.Capacitor,impact(n){const t=this.getEngine();if(!t)return;const o=this.isCapacitor()?n.style.toUpperCase():n.style;t.impact({style:o})},notification(n){const t=this.getEngine();if(!t)return;const o=this.isCapacitor()?n.style.toUpperCase():n.style;t.notification({style:o})},selection(){this.impact({style:"light"})},selectionStart(){const n=this.getEngine();!n||(this.isCapacitor()?n.selectionStart():n.gestureSelectionStart())},selectionChanged(){const n=this.getEngine();!n||(this.isCapacitor()?n.selectionChanged():n.gestureSelectionChanged())},selectionEnd(){const n=this.getEngine();!n||(this.isCapacitor()?n.selectionEnd():n.gestureSelectionEnd())}},m=()=>{e.selection()},g=()=>{e.selectionStart()},d=()=>{e.selectionChanged()},l=()=>{e.selectionEnd()},r=n=>{e.impact(n)}},6465:(E,v,u)=>{u.d(v,{I:()=>l,a:()=>s,b:()=>r,c:()=>f,d:()=>c,f:()=>h,g:()=>o,i:()=>t,p:()=>w,r:()=>y,s:()=>p});var e=u(5861),m=u(3756),g=u(7208);const l="ion-content",r=".ion-content-scroll-host",n=`${l}, ${r}`,t=i=>i&&"ION-CONTENT"===i.tagName,o=function(){var i=(0,e.Z)(function*(_){return t(_)?(yield new Promise(a=>(0,m.c)(_,a)),_.getScrollElement()):_});return function(a){return i.apply(this,arguments)}}(),s=i=>i.querySelector(r)||i.querySelector(n),h=i=>i.closest(n),p=(i,_)=>t(i)?i.scrollToTop(_):Promise.resolve(i.scrollTo({top:0,left:0,behavior:_>0?"smooth":"auto"})),f=(i,_,a,b)=>t(i)?i.scrollByPoint(_,a,b):Promise.resolve(i.scrollBy({top:a,left:_,behavior:b>0?"smooth":"auto"})),w=i=>(0,g.a)(i,l),c=i=>{if(t(i)){const a=i.scrollY;return i.scrollY=!1,a}return i.style.setProperty("overflow","hidden"),!0},y=(i,_)=>{t(i)?i.scrollY=_:i.style.removeProperty("overflow")}},7208:(E,v,u)=>{u.d(v,{a:()=>g,b:()=>m,p:()=>e});const e=d=>console.warn(`[Ionic Warning]: ${d}`),m=(d,...l)=>console.error(`[Ionic Error]: ${d}`,...l),g=(d,...l)=>console.error(`<${d.tagName.toLowerCase()}> must be used inside ${l.join(" or ")}.`)},3230:(E,v,u)=>{u.d(v,{a:()=>e,b:()=>h,c:()=>r,d:()=>p,e:()=>a,f:()=>g,g:()=>m,h:()=>i,i:()=>n,j:()=>o,k:()=>f,l:()=>t,m:()=>l,n:()=>d,o:()=>s,p:()=>w,q:()=>c,r:()=>y,s:()=>_});const e="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Arrow Back</title><path stroke-linecap='square' stroke-miterlimit='10' stroke-width='48' d='M244 400L100 256l144-144M120 256h292' class='ionicon-fill-none'/></svg>",m="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Arrow Down</title><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M112 268l144 144 144-144M256 392V100' class='ionicon-fill-none'/></svg>",g="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Caret Back</title><path d='M368 64L144 256l224 192V64z'/></svg>",d="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Caret Down</title><path d='M64 144l192 224 192-224H64z'/></svg>",l="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Caret Up</title><path d='M448 368L256 144 64 368h384z'/></svg>",r="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Chevron Back</title><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M328 112L184 256l144 144' class='ionicon-fill-none'/></svg>",n="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Chevron Down</title><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M112 184l144 144 144-144' class='ionicon-fill-none'/></svg>",t="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Chevron Forward</title><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M184 112l144 144-144 144' class='ionicon-fill-none'/></svg>",o="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Chevron Forward</title><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M184 112l144 144-144 144' class='ionicon-fill-none'/></svg>",s="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Close</title><path d='M289.94 256l95-95A24 24 0 00351 127l-95 95-95-95a24 24 0 00-34 34l95 95-95 95a24 24 0 1034 34l95-95 95 95a24 24 0 0034-34z'/></svg>",h="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Close Circle</title><path d='M256 48C141.31 48 48 141.31 48 256s93.31 208 208 208 208-93.31 208-208S370.69 48 256 48zm75.31 260.69a16 16 0 11-22.62 22.62L256 278.63l-52.69 52.68a16 16 0 01-22.62-22.62L233.37 256l-52.68-52.69a16 16 0 0122.62-22.62L256 233.37l52.69-52.68a16 16 0 0122.62 22.62L278.63 256z'/></svg>",p="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Close</title><path d='M400 145.49L366.51 112 256 222.51 145.49 112 112 145.49 222.51 256 112 366.51 145.49 400 256 289.49 366.51 400 400 366.51 289.49 256 400 145.49z'/></svg>",f="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Ellipsis Horizontal</title><circle cx='256' cy='256' r='48'/><circle cx='416' cy='256' r='48'/><circle cx='96' cy='256' r='48'/></svg>",w="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Menu</title><path stroke-linecap='round' stroke-miterlimit='10' d='M80 160h352M80 256h352M80 352h352' class='ionicon-fill-none ionicon-stroke-width'/></svg>",c="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Menu</title><path d='M64 384h384v-42.67H64zm0-106.67h384v-42.66H64zM64 128v42.67h384V128z'/></svg>",y="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Reorder Three</title><path stroke-linecap='round' stroke-linejoin='round' d='M96 256h320M96 176h320M96 336h320' class='ionicon-fill-none ionicon-stroke-width'/></svg>",i="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Reorder Two</title><path stroke-linecap='square' stroke-linejoin='round' stroke-width='44' d='M118 304h276M118 208h276' class='ionicon-fill-none'/></svg>",_="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Search</title><path d='M221.09 64a157.09 157.09 0 10157.09 157.09A157.1 157.1 0 00221.09 64z' stroke-miterlimit='10' class='ionicon-fill-none ionicon-stroke-width'/><path stroke-linecap='round' stroke-miterlimit='10' d='M338.29 338.29L448 448' class='ionicon-fill-none ionicon-stroke-width'/></svg>",a="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Search</title><path d='M464 428L339.92 303.9a160.48 160.48 0 0030.72-94.58C370.64 120.37 298.27 48 209.32 48S48 120.37 48 209.32s72.37 161.32 161.32 161.32a160.48 160.48 0 0094.58-30.72L428 464zM209.32 319.69a110.38 110.38 0 11110.37-110.37 110.5 110.5 0 01-110.37 110.37z'/></svg>"},8439:(E,v,u)=>{u.d(v,{s:()=>e});const e=t=>{try{if(t instanceof class n{constructor(o){this.value=o}})return t.value;if(!d()||"string"!=typeof t||""===t)return t;const o=document.createDocumentFragment(),s=document.createElement("div");o.appendChild(s),s.innerHTML=t,r.forEach(w=>{const c=o.querySelectorAll(w);for(let y=c.length-1;y>=0;y--){const i=c[y];i.parentNode?i.parentNode.removeChild(i):o.removeChild(i);const _=g(i);for(let a=0;a<_.length;a++)m(_[a])}});const h=g(o);for(let w=0;w<h.length;w++)m(h[w]);const p=document.createElement("div");p.appendChild(o);const f=p.querySelector("div");return null!==f?f.innerHTML:p.innerHTML}catch(o){return console.error(o),""}},m=t=>{if(t.nodeType&&1!==t.nodeType)return;for(let s=t.attributes.length-1;s>=0;s--){const h=t.attributes.item(s),p=h.name;if(!l.includes(p.toLowerCase())){t.removeAttribute(p);continue}const f=h.value;null!=f&&f.toLowerCase().includes("javascript:")&&t.removeAttribute(p)}const o=g(t);for(let s=0;s<o.length;s++)m(o[s])},g=t=>null!=t.children?t.children:t.childNodes,d=()=>{var t;const s=null===(t=window?.Ionic)||void 0===t?void 0:t.config;return!s||(s.get?s.get("sanitizerEnabled",!0):!0===s.sanitizerEnabled||void 0===s.sanitizerEnabled)},l=["class","id","href","src","name","slot"],r=["script","style","iframe","meta","link","object","embed"]},1316:(E,v,u)=>{u.r(v),u.d(v,{KEYBOARD_DID_CLOSE:()=>m,KEYBOARD_DID_OPEN:()=>e,copyVisualViewport:()=>_,keyboardDidClose:()=>w,keyboardDidOpen:()=>p,keyboardDidResize:()=>f,resetKeyboardAssist:()=>n,setKeyboardClose:()=>h,setKeyboardOpen:()=>s,startKeyboardAssist:()=>t,trackViewportChanges:()=>i});const e="ionKeyboardDidShow",m="ionKeyboardDidHide";let d={},l={},r=!1;const n=()=>{d={},l={},r=!1},t=a=>{o(a),a.visualViewport&&(l=_(a.visualViewport),a.visualViewport.onresize=()=>{i(a),p()||f(a)?s(a):w(a)&&h(a)})},o=a=>{a.addEventListener("keyboardDidShow",b=>s(a,b)),a.addEventListener("keyboardDidHide",()=>h(a))},s=(a,b)=>{c(a,b),r=!0},h=a=>{y(a),r=!1},p=()=>!r&&d.width===l.width&&(d.height-l.height)*l.scale>150,f=a=>r&&!w(a),w=a=>r&&l.height===a.innerHeight,c=(a,b)=>{const x=new CustomEvent(e,{detail:{keyboardHeight:b?b.keyboardHeight:a.innerHeight-l.height}});a.dispatchEvent(x)},y=a=>{const b=new CustomEvent(m);a.dispatchEvent(b)},i=a=>{d=Object.assign({},l),l=_(a.visualViewport)},_=a=>({width:Math.round(a.width),height:Math.round(a.height),offsetTop:a.offsetTop,offsetLeft:a.offsetLeft,pageTop:a.pageTop,pageLeft:a.pageLeft,scale:a.scale})},7741:(E,v,u)=>{u.d(v,{S:()=>m});const m={bubbles:{dur:1e3,circles:9,fn:(g,d,l)=>{const r=g*d/l-g+"ms",n=2*Math.PI*d/l;return{r:5,style:{top:9*Math.sin(n)+"px",left:9*Math.cos(n)+"px","animation-delay":r}}}},circles:{dur:1e3,circles:8,fn:(g,d,l)=>{const r=d/l,n=g*r-g+"ms",t=2*Math.PI*r;return{r:5,style:{top:9*Math.sin(t)+"px",left:9*Math.cos(t)+"px","animation-delay":n}}}},circular:{dur:1400,elmDuration:!0,circles:1,fn:()=>({r:20,cx:48,cy:48,fill:"none",viewBox:"24 24 48 48",transform:"translate(0,0)",style:{}})},crescent:{dur:750,circles:1,fn:()=>({r:26,style:{}})},dots:{dur:750,circles:3,fn:(g,d)=>({r:6,style:{left:9-9*d+"px","animation-delay":-110*d+"ms"}})},lines:{dur:1e3,lines:8,fn:(g,d,l)=>({y1:14,y2:26,style:{transform:`rotate(${360/l*d+(d<l/2?180:-180)}deg)`,"animation-delay":g*d/l-g+"ms"}})},"lines-small":{dur:1e3,lines:8,fn:(g,d,l)=>({y1:12,y2:20,style:{transform:`rotate(${360/l*d+(d<l/2?180:-180)}deg)`,"animation-delay":g*d/l-g+"ms"}})},"lines-sharp":{dur:1e3,lines:12,fn:(g,d,l)=>({y1:17,y2:29,style:{transform:`rotate(${30*d+(d<6?180:-180)}deg)`,"animation-delay":g*d/l-g+"ms"}})},"lines-sharp-small":{dur:1e3,lines:12,fn:(g,d,l)=>({y1:12,y2:20,style:{transform:`rotate(${30*d+(d<6?180:-180)}deg)`,"animation-delay":g*d/l-g+"ms"}})}}},6546:(E,v,u)=>{u.r(v),u.d(v,{createSwipeBackGesture:()=>l});var e=u(3756),m=u(5062),g=u(3139);u(3509);const l=(r,n,t,o,s)=>{const h=r.ownerDocument.defaultView,p=(0,m.i)(r),w=a=>p?-a.deltaX:a.deltaX;return(0,g.createGesture)({el:r,gestureName:"goback-swipe",gesturePriority:40,threshold:10,canStart:a=>(a=>{const{startX:M}=a;return p?M>=h.innerWidth-50:M<=50})(a)&&n(),onStart:t,onMove:a=>{const M=w(a)/h.innerWidth;o(M)},onEnd:a=>{const b=w(a),M=h.innerWidth,x=b/M,D=(a=>p?-a.velocityX:a.velocityX)(a),C=D>=0&&(D>.2||b>M/2),O=(C?1-x:x)*M;let A=0;if(O>5){const L=O/Math.abs(D);A=Math.min(L,540)}s(C,x<=0?.01:(0,e.l)(0,x,.9999),A)}})}},2854:(E,v,u)=>{u.d(v,{c:()=>g,g:()=>l,h:()=>m,o:()=>n});var e=u(5861);const m=(t,o)=>null!==o.closest(t),g=(t,o)=>"string"==typeof t&&t.length>0?Object.assign({"ion-color":!0,[`ion-color-${t}`]:!0},o):o,l=t=>{const o={};return(t=>void 0!==t?(Array.isArray(t)?t:t.split(" ")).filter(s=>null!=s).map(s=>s.trim()).filter(s=>""!==s):[])(t).forEach(s=>o[s]=!0),o},r=/^[a-z][a-z0-9+\-.]*:/,n=function(){var t=(0,e.Z)(function*(o,s,h,p){if(null!=o&&"#"!==o[0]&&!r.test(o)){const f=document.querySelector("ion-router");if(f)return s?.preventDefault(),f.push(o,h,p)}return!1});return function(s,h,p,f){return t.apply(this,arguments)}}()},5051:(E,v,u)=>{u.d(v,{K:()=>l});var e=u(6895),m=u(4719),g=u(9928),d=u(5675);let l=(()=>{class r{}return r.\u0275fac=function(t){return new(t||r)},r.\u0275mod=d.oAB({type:r}),r.\u0275inj=d.cJS({imports:[e.ez,m.u5,g.Pc]}),r})()},2997:(E,v,u)=>{u.d(v,{G:()=>w,X:()=>f});var e=u(5675),m=u(6895),g=u(9928);function d(c,y){if(1&c&&e._UZ(0,"img",7),2&c){const i=e.oxw();e.MGl("src","assets/imgs/",i.gearImage,"",e.LSH)}}function l(c,y){if(1&c&&(e.TgZ(0,"ion-note",11)(1,"span",12),e._uU(2),e.ALo(3,"number"),e._UZ(4,"br"),e._uU(5),e.ALo(6,"number"),e._UZ(7,"br"),e.TgZ(8,"span",13),e._uU(9),e.ALo(10,"number"),e.qZA()()()),2&c){const i=e.oxw(2);e.xp6(2),e.hij("\xa0\xa0\xa0D: ",e.xi3(3,3,i.item.wertung.noteD,"2.3-3")," "),e.xp6(3),e.hij("\xa0\xa0\xa0E: ",e.xi3(6,6,i.item.wertung.noteE,"2.3-3")," "),e.xp6(4),e.Oqu(e.xi3(10,9,i.item.wertung.endnote,"2.3-3"))}}function r(c,y){if(1&c&&(e.TgZ(0,"ion-note",14)(1,"span",12),e._UZ(2,"br"),e._uU(3),e.ALo(4,"number"),e._UZ(5,"br"),e.TgZ(6,"span",15),e._uU(7),e.ALo(8,"number"),e.qZA()()()),2&c){const i=e.oxw(2);e.xp6(3),e.hij("\xa0\xa0\xa0E: ",e.xi3(4,2,i.item.wertung.noteE,"2.3-3")," "),e.xp6(4),e.Oqu(e.xi3(8,5,i.item.wertung.endnote,"2.3-3"))}}function n(c,y){if(1&c&&(e.TgZ(0,"ion-note",14)(1,"span",15),e._uU(2),e.ALo(3,"number"),e.qZA()()),2&c){const i=e.oxw(2);e.xp6(2),e.Oqu(e.xi3(3,1,i.item.wertung.endnote,"2.3-3"))}}function t(c,y){if(1&c&&(e.TgZ(0,"ion-col",8),e.YNc(1,l,11,12,"ion-note",9),e.YNc(2,r,9,8,"ion-note",10),e.YNc(3,n,4,4,"ion-note",10),e.qZA()),2&c){const i=e.oxw();e.xp6(1),e.Q6J("ngIf",i.item.isDNoteUsed&&void 0!==i.item.wertung.noteD),e.xp6(1),e.Q6J("ngIf",!i.item.isDNoteUsed&&i.item.wertung.noteE!==i.item.wertung.endnote),e.xp6(1),e.Q6J("ngIf",!i.item.isDNoteUsed&&i.item.wertung.noteE===i.item.wertung.endnote)}}function o(c,y){if(1&c&&(e.TgZ(0,"ion-note",14)(1,"span",12),e._uU(2),e.ALo(3,"number"),e._UZ(4,"br"),e._uU(5),e.ALo(6,"number"),e._UZ(7,"br"),e.qZA(),e.TgZ(8,"span",15),e._uU(9),e.ALo(10,"number"),e.qZA()()),2&c){const i=e.oxw(2);e.xp6(2),e.hij("D: ",e.xi3(3,3,i.item.wertung.noteD,"2.3-3"),"\xa0\xa0 "),e.xp6(3),e.hij("E: ",e.xi3(6,6,i.item.wertung.noteE,"2.3-3"),"\xa0\xa0 "),e.xp6(4),e.hij("",e.xi3(10,9,i.item.wertung.endnote,"2.3-3"),"\xa0\xa0")}}function s(c,y){if(1&c&&(e.TgZ(0,"ion-note",14),e._UZ(1,"br"),e._uU(2),e.ALo(3,"number"),e._UZ(4,"br"),e.TgZ(5,"span",15),e._uU(6),e.ALo(7,"number"),e.qZA()()),2&c){const i=e.oxw(2);e.xp6(2),e.hij("E: ",e.xi3(3,2,i.item.wertung.noteE,"2.3-3"),"\xa0\xa0 "),e.xp6(4),e.hij("",e.xi3(7,5,i.item.wertung.endnote,"2.3-3"),"\xa0\xa0")}}function h(c,y){if(1&c&&(e.TgZ(0,"ion-note",14)(1,"span",15),e._uU(2),e.ALo(3,"number"),e.qZA()()),2&c){const i=e.oxw(2);e.xp6(2),e.hij("",e.xi3(3,1,i.item.wertung.endnote,"2.3-3"),"\xa0\xa0")}}function p(c,y){if(1&c&&(e.TgZ(0,"ion-row")(1,"ion-col",16)(2,"div",17)(3,"div",18),e._uU(4),e.qZA()(),e.TgZ(5,"div",17),e._uU(6),e.qZA()(),e.TgZ(7,"ion-col",19),e.YNc(8,o,11,12,"ion-note",10),e.YNc(9,s,8,8,"ion-note",10),e.YNc(10,h,4,4,"ion-note",10),e.qZA()()),2&c){const i=e.oxw();e.xp6(4),e.Oqu(i.item.vorname+" "+i.item.name),e.xp6(2),e.hij(" ",i.item.verein," "),e.xp6(2),e.Q6J("ngIf",i.item.isDNoteUsed&&void 0!==i.item.wertung.noteD),e.xp6(1),e.Q6J("ngIf",!i.item.isDNoteUsed&&i.item.wertung.noteE!==i.item.wertung.endnote),e.xp6(1),e.Q6J("ngIf",!i.item.isDNoteUsed&&i.item.wertung.noteE===i.item.wertung.endnote)}}var f=(()=>{return(c=f||(f={}))[c.NONE=0]="NONE",c[c.ATHLET=1]="ATHLET",c[c.PROGRAMM=2]="PROGRAMM",f;var c})();let w=(()=>{class c{constructor(){this.groupBy=f,this.groupedBy=f.NONE,this.gearMapping={1:"boden.svg",2:"pferdpauschen.svg",3:"ringe.svg",4:"sprung.svg",5:"barren.svg",6:"reck.svg",26:"ringe.svg",27:"stufenbarren.svg",28:"schwebebalken.svg",29:"minitramp"}}get titlestart(){return this.title.split(" - ")[0]}get titlerest(){return this.title.split(" - ")[1]}get gearImage(){return this.gearMapping[this.item.geraet]||void 0}ngOnInit(){}}return c.\u0275fac=function(i){return new(i||c)},c.\u0275cmp=e.Xpm({type:c,selectors:[["result-display"]],inputs:{item:"item",title:"title",groupedBy:"groupedBy"},decls:12,vars:5,consts:[["align-self-top","","size-xs","8","size-md","8","size-lg","8","size-xl","8",1,"nonwrapping"],[1,"img-wrapper"],[3,"src",4,"ngIf"],[1,"fixedheight",2,"font-size","20pt"],[1,"fixedheight",2,"font-size","12pt"],["col-auto","","align-self-end","",4,"ngIf"],[4,"ngIf"],[3,"src"],["col-auto","","align-self-end",""],["item-text-wrap","",4,"ngIf"],["item-end","","item-text-wrap","",4,"ngIf"],["item-text-wrap",""],[1,"rightbound"],[2,"font-size","18pt","font-weight","bold"],["item-end","","item-text-wrap",""],[1,"rightbound",2,"font-size","18pt","font-weight","bold"],["size-xs","8","size-md","8","size-lg","8","size-xl","8"],[1,"nonwrapping"],[2,"font-size","16pt"],["col-auto",""]],template:function(i,_){1&i&&(e.TgZ(0,"ion-card")(1,"ion-grid")(2,"ion-row")(3,"ion-col",0)(4,"div",1),e.YNc(5,d,1,1,"img",2),e.qZA(),e.TgZ(6,"div",3),e._uU(7),e.qZA(),e.TgZ(8,"div",4),e._uU(9),e.qZA()(),e.YNc(10,t,4,3,"ion-col",5),e.qZA(),e.YNc(11,p,11,5,"ion-row",6),e.qZA()()),2&i&&(e.xp6(5),e.Q6J("ngIf",_.gearImage),e.xp6(2),e.Oqu(_.titlestart),e.xp6(2),e.Oqu(_.titlerest),e.xp6(1),e.Q6J("ngIf",_.groupedBy===_.groupBy.ATHLET),e.xp6(1),e.Q6J("ngIf",_.groupedBy!==_.groupBy.ATHLET))},dependencies:[m.O5,g.PM,g.wI,g.jY,g.uN,g.Nd,m.JJ],styles:["ion-card[_ngcontent-%COMP%]{margin:auto;position:relative;color:var(--ion-card-color);background-color:var(--ion-card-background-color);border-radius:4px;box-shadow:0 3px 1px -2px var(--ion-card-shadow-color1),0 2px 2px 0 var(--ion-card-shadow-color2),0 1px 5px 0 var(--ion-card-shadow-color3)}.fixedheight[_ngcontent-%COMP%]{word-wrap:normal;position:relative;left:50px}.img-wrapper[_ngcontent-%COMP%]{position:absolute;left:0px;top:0px}.img-wrapper[_ngcontent-%COMP%]   img[_ngcontent-%COMP%]{border-radius:5px;width:50px;height:55px;background-color:var(--ion-color-step-500)}.rightbound[_ngcontent-%COMP%]{float:right}.nonwrapping[_ngcontent-%COMP%]{width:100%;display:inline-block;white-space:nowrap}"]}),c})()},13:(E,v,u)=>{u.d(v,{b:()=>g});var e=u(3489),m=u(6014);function g(n,t=m.P){return o=>o.lift(new d(n,t))}class d{constructor(t,o){this.dueTime=t,this.scheduler=o}call(t,o){return o.subscribe(new l(t,this.dueTime,this.scheduler))}}class l extends e.L{constructor(t,o,s){super(t),this.dueTime=o,this.scheduler=s,this.debouncedSubscription=null,this.lastValue=null,this.hasValue=!1}_next(t){this.clearDebounce(),this.lastValue=t,this.hasValue=!0,this.add(this.debouncedSubscription=this.scheduler.schedule(r,this.dueTime,this))}_complete(){this.debouncedNext(),this.destination.complete()}debouncedNext(){if(this.clearDebounce(),this.hasValue){const{lastValue:t}=this;this.lastValue=null,this.hasValue=!1,this.destination.next(t)}}clearDebounce(){const t=this.debouncedSubscription;null!==t&&(this.remove(t),t.unsubscribe(),this.debouncedSubscription=null)}}function r(n){n.debouncedNext()}}}]);