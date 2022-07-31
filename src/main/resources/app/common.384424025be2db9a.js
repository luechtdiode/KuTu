"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[8592],{4302:(b,f,l)=>{l.d(f,{N1:()=>r});var g=l(655),e=l(5419),p=l(5675),r=function(n){function t(){return null!==n&&n.apply(this,arguments)||this}return(0,g.ZT)(t,n),t.prototype.hideFormAccessoryBar=function(i){return(0,e.DM)(this,"hideFormAccessoryBar",{sync:!0,platforms:["iOS"]},arguments)},t.prototype.hide=function(){return(0,e.DM)(this,"hide",{sync:!0,platforms:["iOS","Android"]},arguments)},t.prototype.show=function(){return(0,e.DM)(this,"show",{sync:!0,platforms:["Android"]},arguments)},t.prototype.setResizeMode=function(i){return(0,e.DM)(this,"setResizeMode",{sync:!0,platforms:["iOS"]},arguments)},t.prototype.setKeyboardStyle=function(i){return(0,e.DM)(this,"setKeyboardStyle",{sync:!0,platforms:["iOS"]},arguments)},t.prototype.disableScroll=function(i){return(0,e.DM)(this,"disableScroll",{sync:!0,platforms:["iOS"]},arguments)},t.prototype.onKeyboardShow=function(){return(0,e.DM)(this,"onKeyboardShow",{eventObservable:!0,event:"native.keyboardshow",platforms:["iOS","Android"]},arguments)},t.prototype.onKeyboardWillShow=function(){return(0,e.DM)(this,"onKeyboardWillShow",{eventObservable:!0,event:"keyboardWillShow",platforms:["iOS","Android"]},arguments)},t.prototype.onKeyboardDidShow=function(){return(0,e.DM)(this,"onKeyboardDidShow",{eventObservable:!0,event:"keyboardDidShow",platforms:["iOS","Android"]},arguments)},t.prototype.onKeyboardHide=function(){return(0,e.DM)(this,"onKeyboardHide",{eventObservable:!0,event:"native.keyboardhide",platforms:["iOS","Android"]},arguments)},t.prototype.onKeyboardWillHide=function(){return(0,e.DM)(this,"onKeyboardWillHide",{eventObservable:!0,event:"keyboardWillHide",platforms:["iOS","Android"]},arguments)},t.prototype.onKeyboardDidHide=function(){return(0,e.DM)(this,"onKeyboardDidHide",{eventObservable:!0,event:"keyboardDidHide",platforms:["iOS","Android"]},arguments)},Object.defineProperty(t.prototype,"isVisible",{get:function(){return(0,e.J2)(this,"isVisible")},set:function(i){(0,e.Iq)(this,"isVisible",i)},enumerable:!1,configurable:!0}),t.pluginName="Keyboard",t.plugin="cordova-plugin-ionic-keyboard",t.pluginRef="window.Keyboard",t.repo="https://github.com/ionic-team/cordova-plugin-ionic-keyboard",t.platforms=["Android","iOS"],t.\u0275fac=function(){var i;return function(h){return(i||(i=p.n5z(t)))(h||t)}}(),t.\u0275prov=p.Yz7({token:t,factory:function(i){return t.\u0275fac(i)}}),t}(e.KY)},4833:(b,f,l)=>{l.d(f,{c:()=>c});var g=l(7205),e=l(7683),p=l(3139);const c=(u,r)=>{let n,t;const i=(d,_,v)=>{if(typeof document>"u")return;const w=document.elementFromPoint(d,_);w&&r(w)?w!==n&&(h(),a(w,v)):h()},a=(d,_)=>{n=d,t||(t=n);const v=n;(0,g.c)(()=>v.classList.add("ion-activated")),_()},h=(d=!1)=>{if(!n)return;const _=n;(0,g.c)(()=>_.classList.remove("ion-activated")),d&&t!==n&&n.click(),n=void 0};return(0,p.createGesture)({el:u,gestureName:"buttonActiveDrag",threshold:0,onStart:d=>i(d.currentX,d.currentY,e.a),onMove:d=>i(d.currentX,d.currentY,e.b),onEnd:()=>{h(!0),(0,e.h)(),t=void 0}})}},8685:(b,f,l)=>{l.d(f,{g:()=>g});const g=(r,n,t,i,a)=>p(r[1],n[1],t[1],i[1],a).map(h=>e(r[0],n[0],t[0],i[0],h)),e=(r,n,t,i,a)=>a*(3*n*Math.pow(a-1,2)+a*(-3*t*a+3*t+i*a))-r*Math.pow(a-1,3),p=(r,n,t,i,a)=>u((i-=a)-3*(t-=a)+3*(n-=a)-(r-=a),3*t-6*n+3*r,3*n-3*r,r).filter(d=>d>=0&&d<=1),u=(r,n,t,i)=>{if(0===r)return((r,n,t)=>{const i=n*n-4*r*t;return i<0?[]:[(-n+Math.sqrt(i))/(2*r),(-n-Math.sqrt(i))/(2*r)]})(n,t,i);const a=(3*(t/=r)-(n/=r)*n)/3,h=(2*n*n*n-9*n*t+27*(i/=r))/27;if(0===a)return[Math.pow(-h,1/3)];if(0===h)return[Math.sqrt(-a),-Math.sqrt(-a)];const d=Math.pow(h/2,2)+Math.pow(a/3,3);if(0===d)return[Math.pow(h/2,.5)-n/3];if(d>0)return[Math.pow(-h/2+Math.sqrt(d),1/3)-Math.pow(h/2+Math.sqrt(d),1/3)-n/3];const _=Math.sqrt(Math.pow(-a/3,3)),v=Math.acos(-h/(2*Math.sqrt(Math.pow(-a/3,3)))),w=2*Math.pow(_,1/3);return[w*Math.cos(v/3)-n/3,w*Math.cos((v+2*Math.PI)/3)-n/3,w*Math.cos((v+4*Math.PI)/3)-n/3]}},5062:(b,f,l)=>{l.d(f,{i:()=>g});const g=e=>e&&""!==e.dir?"rtl"===e.dir.toLowerCase():"rtl"===document?.dir.toLowerCase()},1112:(b,f,l)=>{l.r(f),l.d(f,{startFocusVisible:()=>c});const g="ion-focused",p=["Tab","ArrowDown","Space","Escape"," ","Shift","Enter","ArrowLeft","ArrowRight","ArrowUp","Home","End"],c=u=>{let r=[],n=!0;const t=u?u.shadowRoot:document,i=u||document.body,a=y=>{r.forEach(s=>s.classList.remove(g)),y.forEach(s=>s.classList.add(g)),r=y},h=()=>{n=!1,a([])},d=y=>{n=p.includes(y.key),n||a([])},_=y=>{if(n&&y.composedPath){const s=y.composedPath().filter(m=>!!m.classList&&m.classList.contains("ion-focusable"));a(s)}},v=()=>{t.activeElement===i&&a([])};return t.addEventListener("keydown",d),t.addEventListener("focusin",_),t.addEventListener("focusout",v),t.addEventListener("touchstart",h),t.addEventListener("mousedown",h),{destroy:()=>{t.removeEventListener("keydown",d),t.removeEventListener("focusin",_),t.removeEventListener("focusout",v),t.removeEventListener("touchstart",h),t.removeEventListener("mousedown",h)},setFocus:a}}},1878:(b,f,l)=>{l.d(f,{C:()=>u,a:()=>p,d:()=>c});var g=l(5861),e=l(3756);const p=function(){var r=(0,g.Z)(function*(n,t,i,a,h,d){var _;if(n)return n.attachViewToDom(t,i,h,a);if(!(d||"string"==typeof i||i instanceof HTMLElement))throw new Error("framework delegate is missing");const v="string"==typeof i?null===(_=t.ownerDocument)||void 0===_?void 0:_.createElement(i):i;return a&&a.forEach(w=>v.classList.add(w)),h&&Object.assign(v,h),t.appendChild(v),yield new Promise(w=>(0,e.c)(v,w)),v});return function(t,i,a,h,d,_){return r.apply(this,arguments)}}(),c=(r,n)=>{if(n){if(r)return r.removeViewFromDom(n.parentElement,n);n.remove()}return Promise.resolve()},u=()=>{let r,n;return{attachViewToDom:function(){var a=(0,g.Z)(function*(h,d,_={},v=[]){var w,y;if(r=h,d){const m="string"==typeof d?null===(w=r.ownerDocument)||void 0===w?void 0:w.createElement(d):d;v.forEach(o=>m.classList.add(o)),Object.assign(m,_),r.appendChild(m),yield new Promise(o=>(0,e.c)(m,o))}else if(r.children.length>0){const m=null===(y=r.ownerDocument)||void 0===y?void 0:y.createElement("div");v.forEach(o=>m.classList.add(o)),m.append(...r.children),r.appendChild(m)}const s=document.querySelector("ion-app")||document.body;return n=document.createComment("ionic teleport"),r.parentNode.insertBefore(n,r),s.appendChild(r),r});return function(d,_){return a.apply(this,arguments)}}(),removeViewFromDom:()=>(r&&n&&(n.parentNode.insertBefore(r,n),n.remove()),Promise.resolve())}}},7683:(b,f,l)=>{l.d(f,{a:()=>p,b:()=>c,c:()=>e,d:()=>r,h:()=>u});const g={getEngine(){var n;const t=window;return t.TapticEngine||(null===(n=t.Capacitor)||void 0===n?void 0:n.isPluginAvailable("Haptics"))&&t.Capacitor.Plugins.Haptics},available(){return!!this.getEngine()},isCordova:()=>!!window.TapticEngine,isCapacitor:()=>!!window.Capacitor,impact(n){const t=this.getEngine();if(!t)return;const i=this.isCapacitor()?n.style.toUpperCase():n.style;t.impact({style:i})},notification(n){const t=this.getEngine();if(!t)return;const i=this.isCapacitor()?n.style.toUpperCase():n.style;t.notification({style:i})},selection(){this.impact({style:"light"})},selectionStart(){const n=this.getEngine();!n||(this.isCapacitor()?n.selectionStart():n.gestureSelectionStart())},selectionChanged(){const n=this.getEngine();!n||(this.isCapacitor()?n.selectionChanged():n.gestureSelectionChanged())},selectionEnd(){const n=this.getEngine();!n||(this.isCapacitor()?n.selectionEnd():n.gestureSelectionEnd())}},e=()=>{g.selection()},p=()=>{g.selectionStart()},c=()=>{g.selectionChanged()},u=()=>{g.selectionEnd()},r=n=>{g.impact(n)}},6465:(b,f,l)=>{l.d(f,{I:()=>u,a:()=>a,b:()=>r,c:()=>_,d:()=>w,f:()=>h,g:()=>i,i:()=>t,p:()=>v,r:()=>y,s:()=>d});var g=l(5861),e=l(3756),p=l(7208);const u="ion-content",r=".ion-content-scroll-host",n=`${u}, ${r}`,t=s=>s&&"ION-CONTENT"===s.tagName,i=function(){var s=(0,g.Z)(function*(m){return t(m)?(yield new Promise(o=>(0,e.c)(m,o)),m.getScrollElement()):m});return function(o){return s.apply(this,arguments)}}(),a=s=>s.querySelector(r)||s.querySelector(n),h=s=>s.closest(n),d=(s,m)=>t(s)?s.scrollToTop(m):Promise.resolve(s.scrollTo({top:0,left:0,behavior:m>0?"smooth":"auto"})),_=(s,m,o,E)=>t(s)?s.scrollByPoint(m,o,E):Promise.resolve(s.scrollBy({top:o,left:m,behavior:E>0?"smooth":"auto"})),v=s=>(0,p.a)(s,u),w=s=>{if(t(s)){const o=s.scrollY;return s.scrollY=!1,o}return s.style.setProperty("overflow","hidden"),!0},y=(s,m)=>{t(s)?s.scrollY=m:s.style.removeProperty("overflow")}},7208:(b,f,l)=>{l.d(f,{a:()=>p,b:()=>e,p:()=>g});const g=c=>console.warn(`[Ionic Warning]: ${c}`),e=(c,...u)=>console.error(`[Ionic Error]: ${c}`,...u),p=(c,...u)=>console.error(`<${c.tagName.toLowerCase()}> must be used inside ${u.join(" or ")}.`)},3230:(b,f,l)=>{l.d(f,{a:()=>g,b:()=>h,c:()=>r,d:()=>d,e:()=>o,f:()=>p,g:()=>e,h:()=>s,i:()=>n,j:()=>i,k:()=>_,l:()=>t,m:()=>u,n:()=>c,o:()=>a,p:()=>v,q:()=>w,r:()=>y,s:()=>m});const g="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Arrow Back</title><path stroke-linecap='square' stroke-miterlimit='10' stroke-width='48' d='M244 400L100 256l144-144M120 256h292' class='ionicon-fill-none'/></svg>",e="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Arrow Down</title><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M112 268l144 144 144-144M256 392V100' class='ionicon-fill-none'/></svg>",p="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Caret Back</title><path d='M368 64L144 256l224 192V64z'/></svg>",c="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Caret Down</title><path d='M64 144l192 224 192-224H64z'/></svg>",u="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Caret Up</title><path d='M448 368L256 144 64 368h384z'/></svg>",r="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Chevron Back</title><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M328 112L184 256l144 144' class='ionicon-fill-none'/></svg>",n="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Chevron Down</title><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M112 184l144 144 144-144' class='ionicon-fill-none'/></svg>",t="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Chevron Forward</title><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M184 112l144 144-144 144' class='ionicon-fill-none'/></svg>",i="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Chevron Forward</title><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M184 112l144 144-144 144' class='ionicon-fill-none'/></svg>",a="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Close</title><path d='M289.94 256l95-95A24 24 0 00351 127l-95 95-95-95a24 24 0 00-34 34l95 95-95 95a24 24 0 1034 34l95-95 95 95a24 24 0 0034-34z'/></svg>",h="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Close Circle</title><path d='M256 48C141.31 48 48 141.31 48 256s93.31 208 208 208 208-93.31 208-208S370.69 48 256 48zm75.31 260.69a16 16 0 11-22.62 22.62L256 278.63l-52.69 52.68a16 16 0 01-22.62-22.62L233.37 256l-52.68-52.69a16 16 0 0122.62-22.62L256 233.37l52.69-52.68a16 16 0 0122.62 22.62L278.63 256z'/></svg>",d="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Close</title><path d='M400 145.49L366.51 112 256 222.51 145.49 112 112 145.49 222.51 256 112 366.51 145.49 400 256 289.49 366.51 400 400 366.51 289.49 256 400 145.49z'/></svg>",_="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Ellipsis Horizontal</title><circle cx='256' cy='256' r='48'/><circle cx='416' cy='256' r='48'/><circle cx='96' cy='256' r='48'/></svg>",v="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Menu</title><path stroke-linecap='round' stroke-miterlimit='10' d='M80 160h352M80 256h352M80 352h352' class='ionicon-fill-none ionicon-stroke-width'/></svg>",w="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Menu</title><path d='M64 384h384v-42.67H64zm0-106.67h384v-42.66H64zM64 128v42.67h384V128z'/></svg>",y="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Reorder Three</title><path stroke-linecap='round' stroke-linejoin='round' d='M96 256h320M96 176h320M96 336h320' class='ionicon-fill-none ionicon-stroke-width'/></svg>",s="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Reorder Two</title><path stroke-linecap='square' stroke-linejoin='round' stroke-width='44' d='M118 304h276M118 208h276' class='ionicon-fill-none'/></svg>",m="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Search</title><path d='M221.09 64a157.09 157.09 0 10157.09 157.09A157.1 157.1 0 00221.09 64z' stroke-miterlimit='10' class='ionicon-fill-none ionicon-stroke-width'/><path stroke-linecap='round' stroke-miterlimit='10' d='M338.29 338.29L448 448' class='ionicon-fill-none ionicon-stroke-width'/></svg>",o="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><title>Search</title><path d='M464 428L339.92 303.9a160.48 160.48 0 0030.72-94.58C370.64 120.37 298.27 48 209.32 48S48 120.37 48 209.32s72.37 161.32 161.32 161.32a160.48 160.48 0 0094.58-30.72L428 464zM209.32 319.69a110.38 110.38 0 11110.37-110.37 110.5 110.5 0 01-110.37 110.37z'/></svg>"},8439:(b,f,l)=>{l.d(f,{s:()=>g});const g=t=>{try{if(t instanceof class n{constructor(i){this.value=i}})return t.value;if(!c()||"string"!=typeof t||""===t)return t;const i=document.createDocumentFragment(),a=document.createElement("div");i.appendChild(a),a.innerHTML=t,r.forEach(v=>{const w=i.querySelectorAll(v);for(let y=w.length-1;y>=0;y--){const s=w[y];s.parentNode?s.parentNode.removeChild(s):i.removeChild(s);const m=p(s);for(let o=0;o<m.length;o++)e(m[o])}});const h=p(i);for(let v=0;v<h.length;v++)e(h[v]);const d=document.createElement("div");d.appendChild(i);const _=d.querySelector("div");return null!==_?_.innerHTML:d.innerHTML}catch(i){return console.error(i),""}},e=t=>{if(t.nodeType&&1!==t.nodeType)return;for(let a=t.attributes.length-1;a>=0;a--){const h=t.attributes.item(a),d=h.name;if(!u.includes(d.toLowerCase())){t.removeAttribute(d);continue}const _=h.value;null!=_&&_.toLowerCase().includes("javascript:")&&t.removeAttribute(d)}const i=p(t);for(let a=0;a<i.length;a++)e(i[a])},p=t=>null!=t.children?t.children:t.childNodes,c=()=>{var t;const a=null===(t=window?.Ionic)||void 0===t?void 0:t.config;return!a||(a.get?a.get("sanitizerEnabled",!0):!0===a.sanitizerEnabled||void 0===a.sanitizerEnabled)},u=["class","id","href","src","name","slot"],r=["script","style","iframe","meta","link","object","embed"]},1316:(b,f,l)=>{l.r(f),l.d(f,{KEYBOARD_DID_CLOSE:()=>e,KEYBOARD_DID_OPEN:()=>g,copyVisualViewport:()=>m,keyboardDidClose:()=>v,keyboardDidOpen:()=>d,keyboardDidResize:()=>_,resetKeyboardAssist:()=>n,setKeyboardClose:()=>h,setKeyboardOpen:()=>a,startKeyboardAssist:()=>t,trackViewportChanges:()=>s});const g="ionKeyboardDidShow",e="ionKeyboardDidHide";let c={},u={},r=!1;const n=()=>{c={},u={},r=!1},t=o=>{i(o),o.visualViewport&&(u=m(o.visualViewport),o.visualViewport.onresize=()=>{s(o),d()||_(o)?a(o):v(o)&&h(o)})},i=o=>{o.addEventListener("keyboardDidShow",E=>a(o,E)),o.addEventListener("keyboardDidHide",()=>h(o))},a=(o,E)=>{w(o,E),r=!0},h=o=>{y(o),r=!1},d=()=>!r&&c.width===u.width&&(c.height-u.height)*u.scale>150,_=o=>r&&!v(o),v=o=>r&&u.height===o.innerHeight,w=(o,E)=>{const M=new CustomEvent(g,{detail:{keyboardHeight:E?E.keyboardHeight:o.innerHeight-u.height}});o.dispatchEvent(M)},y=o=>{const E=new CustomEvent(e);o.dispatchEvent(E)},s=o=>{c=Object.assign({},u),u=m(o.visualViewport)},m=o=>({width:Math.round(o.width),height:Math.round(o.height),offsetTop:o.offsetTop,offsetLeft:o.offsetLeft,pageTop:o.pageTop,pageLeft:o.pageLeft,scale:o.scale})},7741:(b,f,l)=>{l.d(f,{S:()=>e});const e={bubbles:{dur:1e3,circles:9,fn:(p,c,u)=>{const r=p*c/u-p+"ms",n=2*Math.PI*c/u;return{r:5,style:{top:9*Math.sin(n)+"px",left:9*Math.cos(n)+"px","animation-delay":r}}}},circles:{dur:1e3,circles:8,fn:(p,c,u)=>{const r=c/u,n=p*r-p+"ms",t=2*Math.PI*r;return{r:5,style:{top:9*Math.sin(t)+"px",left:9*Math.cos(t)+"px","animation-delay":n}}}},circular:{dur:1400,elmDuration:!0,circles:1,fn:()=>({r:20,cx:48,cy:48,fill:"none",viewBox:"24 24 48 48",transform:"translate(0,0)",style:{}})},crescent:{dur:750,circles:1,fn:()=>({r:26,style:{}})},dots:{dur:750,circles:3,fn:(p,c)=>({r:6,style:{left:9-9*c+"px","animation-delay":-110*c+"ms"}})},lines:{dur:1e3,lines:8,fn:(p,c,u)=>({y1:14,y2:26,style:{transform:`rotate(${360/u*c+(c<u/2?180:-180)}deg)`,"animation-delay":p*c/u-p+"ms"}})},"lines-small":{dur:1e3,lines:8,fn:(p,c,u)=>({y1:12,y2:20,style:{transform:`rotate(${360/u*c+(c<u/2?180:-180)}deg)`,"animation-delay":p*c/u-p+"ms"}})},"lines-sharp":{dur:1e3,lines:12,fn:(p,c,u)=>({y1:17,y2:29,style:{transform:`rotate(${30*c+(c<6?180:-180)}deg)`,"animation-delay":p*c/u-p+"ms"}})},"lines-sharp-small":{dur:1e3,lines:12,fn:(p,c,u)=>({y1:12,y2:20,style:{transform:`rotate(${30*c+(c<6?180:-180)}deg)`,"animation-delay":p*c/u-p+"ms"}})}}},6546:(b,f,l)=>{l.r(f),l.d(f,{createSwipeBackGesture:()=>u});var g=l(3756),e=l(5062),p=l(3139);l(3509);const u=(r,n,t,i,a)=>{const h=r.ownerDocument.defaultView,d=(0,e.i)(r),v=o=>d?-o.deltaX:o.deltaX;return(0,p.createGesture)({el:r,gestureName:"goback-swipe",gesturePriority:40,threshold:10,canStart:o=>(o=>{const{startX:x}=o;return d?x>=h.innerWidth-50:x<=50})(o)&&n(),onStart:t,onMove:o=>{const x=v(o)/h.innerWidth;i(x)},onEnd:o=>{const E=v(o),x=h.innerWidth,M=E/x,D=(o=>d?-o.velocityX:o.velocityX)(o),C=D>=0&&(D>.2||E>x/2),O=(C?1-M:M)*x;let T=0;if(O>5){const L=O/Math.abs(D);T=Math.min(L,540)}a(C,M<=0?.01:(0,g.l)(0,M,.9999),T)}})}},2854:(b,f,l)=>{l.d(f,{c:()=>p,g:()=>u,h:()=>e,o:()=>n});var g=l(5861);const e=(t,i)=>null!==i.closest(t),p=(t,i)=>"string"==typeof t&&t.length>0?Object.assign({"ion-color":!0,[`ion-color-${t}`]:!0},i):i,u=t=>{const i={};return(t=>void 0!==t?(Array.isArray(t)?t:t.split(" ")).filter(a=>null!=a).map(a=>a.trim()).filter(a=>""!==a):[])(t).forEach(a=>i[a]=!0),i},r=/^[a-z][a-z0-9+\-.]*:/,n=function(){var t=(0,g.Z)(function*(i,a,h,d){if(null!=i&&"#"!==i[0]&&!r.test(i)){const _=document.querySelector("ion-router");if(_)return a?.preventDefault(),_.push(i,h,d)}return!1});return function(a,h,d,_){return t.apply(this,arguments)}}()},5051:(b,f,l)=>{l.d(f,{K:()=>u});var g=l(6895),e=l(4719),p=l(9928),c=l(5675);let u=(()=>{class r{}return r.\u0275fac=function(t){return new(t||r)},r.\u0275mod=c.oAB({type:r}),r.\u0275inj=c.cJS({imports:[g.ez,e.u5,p.Pc]}),r})()},2997:(b,f,l)=>{l.d(f,{G:()=>y,X:()=>w});var g=l(7225),e=l(5675),p=l(6895),c=l(9928);function u(s,m){if(1&s&&(e.TgZ(0,"ion-col",3),e._UZ(1,"ion-icon",4),e.TgZ(2,"div",5),e._uU(3),e.qZA(),e.TgZ(4,"div",6),e._uU(5),e.qZA()()),2&s){const o=e.oxw();e.xp6(1),e.MGl("src","assets/imgs/",o.gearImage,""),e.xp6(2),e.Oqu(o.titlestart),e.xp6(2),e.Oqu(o.titlerest)}}function r(s,m){if(1&s&&(e.TgZ(0,"ion-col",3)(1,"div",7),e._uU(2),e.qZA(),e.TgZ(3,"div",8),e._uU(4),e.qZA()()),2&s){const o=e.oxw();e.xp6(2),e.Oqu(o.titlestart),e.xp6(2),e.Oqu(o.titlerest)}}function n(s,m){if(1&s&&(e.TgZ(0,"ion-note",12)(1,"span",13),e._uU(2),e.ALo(3,"number"),e._UZ(4,"br"),e._uU(5),e.ALo(6,"number"),e._UZ(7,"br"),e.TgZ(8,"span",14),e._uU(9),e.ALo(10,"number"),e.qZA()()()),2&s){const o=e.oxw(2);e.xp6(2),e.hij("\xa0\xa0\xa0D: ",e.xi3(3,3,o.item.wertung.noteD,"2.3-3")," "),e.xp6(3),e.hij("\xa0\xa0\xa0E: ",e.xi3(6,6,o.item.wertung.noteE,"2.3-3")," "),e.xp6(4),e.Oqu(e.xi3(10,9,o.item.wertung.endnote,"2.3-3"))}}function t(s,m){if(1&s&&(e.TgZ(0,"ion-note",15)(1,"span",13),e._UZ(2,"br"),e._uU(3),e.ALo(4,"number"),e._UZ(5,"br"),e.TgZ(6,"span",16),e._uU(7),e.ALo(8,"number"),e.qZA()()()),2&s){const o=e.oxw(2);e.xp6(3),e.hij("\xa0\xa0\xa0E: ",e.xi3(4,2,o.item.wertung.noteE,"2.3-3")," "),e.xp6(4),e.Oqu(e.xi3(8,5,o.item.wertung.endnote,"2.3-3"))}}function i(s,m){if(1&s&&(e.TgZ(0,"ion-note",15)(1,"span",16),e._uU(2),e.ALo(3,"number"),e.qZA()()),2&s){const o=e.oxw(2);e.xp6(2),e.Oqu(e.xi3(3,1,o.item.wertung.endnote,"2.3-3"))}}function a(s,m){if(1&s&&(e.TgZ(0,"ion-col",9),e.YNc(1,n,11,12,"ion-note",10),e.YNc(2,t,9,8,"ion-note",11),e.YNc(3,i,4,4,"ion-note",11),e.qZA()),2&s){const o=e.oxw();e.xp6(1),e.Q6J("ngIf",o.item.isDNoteUsed&&void 0!==o.item.wertung.noteD),e.xp6(1),e.Q6J("ngIf",!o.item.isDNoteUsed&&o.item.wertung.noteE!==o.item.wertung.endnote),e.xp6(1),e.Q6J("ngIf",!o.item.isDNoteUsed&&o.item.wertung.noteE===o.item.wertung.endnote)}}function h(s,m){if(1&s&&(e.TgZ(0,"ion-note",15)(1,"span",13),e._uU(2),e.ALo(3,"number"),e._UZ(4,"br"),e._uU(5),e.ALo(6,"number"),e._UZ(7,"br"),e.qZA(),e.TgZ(8,"span",16),e._uU(9),e.ALo(10,"number"),e.qZA()()),2&s){const o=e.oxw(2);e.xp6(2),e.hij("D: ",e.xi3(3,3,o.item.wertung.noteD,"2.3-3"),"\xa0\xa0 "),e.xp6(3),e.hij("E: ",e.xi3(6,6,o.item.wertung.noteE,"2.3-3"),"\xa0\xa0 "),e.xp6(4),e.hij("",e.xi3(10,9,o.item.wertung.endnote,"2.3-3"),"\xa0\xa0")}}function d(s,m){if(1&s&&(e.TgZ(0,"ion-note",15),e._UZ(1,"br"),e._uU(2),e.ALo(3,"number"),e._UZ(4,"br"),e.TgZ(5,"span",16),e._uU(6),e.ALo(7,"number"),e.qZA()()),2&s){const o=e.oxw(2);e.xp6(2),e.hij("E: ",e.xi3(3,2,o.item.wertung.noteE,"2.3-3"),"\xa0\xa0 "),e.xp6(4),e.hij("",e.xi3(7,5,o.item.wertung.endnote,"2.3-3"),"\xa0\xa0")}}function _(s,m){if(1&s&&(e.TgZ(0,"ion-note",15)(1,"span",16),e._uU(2),e.ALo(3,"number"),e.qZA()()),2&s){const o=e.oxw(2);e.xp6(2),e.hij("",e.xi3(3,1,o.item.wertung.endnote,"2.3-3"),"\xa0\xa0")}}function v(s,m){if(1&s&&(e.TgZ(0,"ion-row")(1,"ion-col",17)(2,"div",18)(3,"div",19),e._uU(4),e.qZA()(),e.TgZ(5,"div",18),e._uU(6),e.qZA()(),e.TgZ(7,"ion-col",20),e.YNc(8,h,11,12,"ion-note",11),e.YNc(9,d,8,8,"ion-note",11),e.YNc(10,_,4,4,"ion-note",11),e.qZA()()),2&s){const o=e.oxw();e.xp6(4),e.Oqu(o.item.vorname+" "+o.item.name),e.xp6(2),e.hij(" ",o.item.verein," "),e.xp6(2),e.Q6J("ngIf",o.item.isDNoteUsed&&void 0!==o.item.wertung.noteD),e.xp6(1),e.Q6J("ngIf",!o.item.isDNoteUsed&&o.item.wertung.noteE!==o.item.wertung.endnote),e.xp6(1),e.Q6J("ngIf",!o.item.isDNoteUsed&&o.item.wertung.noteE===o.item.wertung.endnote)}}var w=(()=>{return(s=w||(w={}))[s.NONE=0]="NONE",s[s.ATHLET=1]="ATHLET",s[s.PROGRAMM=2]="PROGRAMM",w;var s})();let y=(()=>{class s{constructor(){this.groupBy=w,this.groupedBy=w.NONE}get titlestart(){return this.title.split(" - ")[0]}get titlerest(){return this.title.split(" - ")[1]}get gearImage(){return g.WZ[this.item.geraet]||void 0}ngOnInit(){}}return s.\u0275fac=function(o){return new(o||s)},s.\u0275cmp=e.Xpm({type:s,selectors:[["result-display"]],inputs:{item:"item",title:"title",groupedBy:"groupedBy"},decls:7,vars:4,consts:[["align-self-top","","class","nonwrapping","size-xs","8","size-md","8","size-lg","8","size-xl","8",4,"ngIf"],["col-auto","","align-self-end","",4,"ngIf"],[4,"ngIf"],["align-self-top","","size-xs","8","size-md","8","size-lg","8","size-xl","8",1,"nonwrapping"],[1,"img-wrapper",3,"src"],[1,"after-icon","fixedheight",2,"font-size","18pt"],[1,"after-icon","fixedheight",2,"font-size","12pt"],[2,"font-size","18pt"],[2,"font-size","12pt"],["col-auto","","align-self-end",""],["item-text-wrap","",4,"ngIf"],["item-end","","item-text-wrap","",4,"ngIf"],["item-text-wrap",""],[1,"rightbound"],[2,"font-size","18pt","font-weight","bold"],["item-end","","item-text-wrap",""],[1,"rightbound",2,"font-size","18pt","font-weight","bold"],["size-xs","8","size-md","8","size-lg","8","size-xl","8"],[1,"nonwrapping"],[2,"font-size","16pt"],["col-auto",""]],template:function(o,E){1&o&&(e.TgZ(0,"ion-card")(1,"ion-grid")(2,"ion-row"),e.YNc(3,u,6,3,"ion-col",0),e.YNc(4,r,5,2,"ion-col",0),e.YNc(5,a,4,3,"ion-col",1),e.qZA(),e.YNc(6,v,11,5,"ion-row",2),e.qZA()()),2&o&&(e.xp6(3),e.Q6J("ngIf",E.gearImage),e.xp6(1),e.Q6J("ngIf",!E.gearImage),e.xp6(1),e.Q6J("ngIf",E.groupedBy===E.groupBy.ATHLET),e.xp6(1),e.Q6J("ngIf",E.groupedBy!==E.groupBy.ATHLET))},dependencies:[p.O5,c.PM,c.wI,c.jY,c.gu,c.uN,c.Nd,p.JJ],styles:["ion-card[_ngcontent-%COMP%]{margin:auto;position:relative;color:var(--ion-card-color);background-color:var(--ion-card-background-color);border-radius:4px;box-shadow:0 3px 1px -2px var(--ion-card-shadow-color1),0 2px 2px 0 var(--ion-card-shadow-color2),0 1px 5px 0 var(--ion-card-shadow-color3)}ion-icon[_ngcontent-%COMP%]{color:var(--ion-card-color);font-size:50px;border-color:var(--ion-item-border-color);border-width:2px;border-style:solid;border-radius:4px}.fixedheight[_ngcontent-%COMP%]{word-wrap:normal;position:relative}.img-wrapper[_ngcontent-%COMP%]{position:absolute;left:0px;top:4px}.after-icon[_ngcontent-%COMP%]{left:52px}.rightbound[_ngcontent-%COMP%]{float:right}.nonwrapping[_ngcontent-%COMP%]{width:100%;display:inline-block;white-space:nowrap}"]}),s})()},13:(b,f,l)=>{l.d(f,{b:()=>p});var g=l(3489),e=l(6014);function p(n,t=e.P){return i=>i.lift(new c(n,t))}class c{constructor(t,i){this.dueTime=t,this.scheduler=i}call(t,i){return i.subscribe(new u(t,this.dueTime,this.scheduler))}}class u extends g.L{constructor(t,i,a){super(t),this.dueTime=i,this.scheduler=a,this.debouncedSubscription=null,this.lastValue=null,this.hasValue=!1}_next(t){this.clearDebounce(),this.lastValue=t,this.hasValue=!0,this.add(this.debouncedSubscription=this.scheduler.schedule(r,this.dueTime,this))}_complete(){this.debouncedNext(),this.destination.complete()}debouncedNext(){if(this.clearDebounce(),this.hasValue){const{lastValue:t}=this;this.lastValue=null,this.hasValue=!1,this.destination.next(t)}}clearDebounce(){const t=this.debouncedSubscription;null!==t&&(this.remove(t),t.unsubscribe(),this.debouncedSubscription=null)}}function r(n){n.debouncedNext()}}}]);