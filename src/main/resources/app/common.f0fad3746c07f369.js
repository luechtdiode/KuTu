"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[8592],{9262:(y,h,c)=>{c.d(h,{c:()=>l});var f=c(8411),e=c(967),g=c(9203);const l=(a,o)=>{let t,n;const d=(u,w,v)=>{if(typeof document>"u")return;const E=document.elementFromPoint(u,w);E&&o(E)?E!==t&&(i(),_(E,v)):i()},_=(u,w)=>{t=u,n||(n=t);const v=t;(0,f.w)(()=>v.classList.add("ion-activated")),w()},i=(u=!1)=>{if(!t)return;const w=t;(0,f.w)(()=>w.classList.remove("ion-activated")),u&&n!==t&&t.click(),t=void 0};return(0,g.createGesture)({el:a,gestureName:"buttonActiveDrag",threshold:0,onStart:u=>d(u.currentX,u.currentY,e.a),onMove:u=>d(u.currentX,u.currentY,e.b),onEnd:()=>{i(!0),(0,e.h)(),n=void 0}})}},4874:(y,h,c)=>{c.d(h,{g:()=>e});var f=c(9938);const e=()=>{if(void 0!==f.w)return f.w.Capacitor}},5149:(y,h,c)=>{c.d(h,{g:()=>f});const f=(o,t,n,d,_)=>g(o[1],t[1],n[1],d[1],_).map(i=>e(o[0],t[0],n[0],d[0],i)),e=(o,t,n,d,_)=>_*(3*t*Math.pow(_-1,2)+_*(-3*n*_+3*n+d*_))-o*Math.pow(_-1,3),g=(o,t,n,d,_)=>a((d-=_)-3*(n-=_)+3*(t-=_)-(o-=_),3*n-6*t+3*o,3*t-3*o,o).filter(u=>u>=0&&u<=1),a=(o,t,n,d)=>{if(0===o)return((o,t,n)=>{const d=t*t-4*o*n;return d<0?[]:[(-t+Math.sqrt(d))/(2*o),(-t-Math.sqrt(d))/(2*o)]})(t,n,d);const _=(3*(n/=o)-(t/=o)*t)/3,i=(2*t*t*t-9*t*n+27*(d/=o))/27;if(0===_)return[Math.pow(-i,1/3)];if(0===i)return[Math.sqrt(-_),-Math.sqrt(-_)];const u=Math.pow(i/2,2)+Math.pow(_/3,3);if(0===u)return[Math.pow(i/2,.5)-t/3];if(u>0)return[Math.pow(-i/2+Math.sqrt(u),1/3)-Math.pow(i/2+Math.sqrt(u),1/3)-t/3];const w=Math.sqrt(Math.pow(-_/3,3)),v=Math.acos(-i/(2*Math.sqrt(Math.pow(-_/3,3)))),E=2*Math.pow(w,1/3);return[E*Math.cos(v/3)-t/3,E*Math.cos((v+2*Math.PI)/3)-t/3,E*Math.cos((v+4*Math.PI)/3)-t/3]}},5085:(y,h,c)=>{c.d(h,{i:()=>f});const f=e=>e&&""!==e.dir?"rtl"===e.dir.toLowerCase():"rtl"===document?.dir.toLowerCase()},2779:(y,h,c)=>{c.r(h),c.d(h,{startFocusVisible:()=>l});const f="ion-focused",g=["Tab","ArrowDown","Space","Escape"," ","Shift","Enter","ArrowLeft","ArrowRight","ArrowUp","Home","End"],l=a=>{let o=[],t=!0;const n=a?a.shadowRoot:document,d=a||document.body,_=M=>{o.forEach(r=>r.classList.remove(f)),M.forEach(r=>r.classList.add(f)),o=M},i=()=>{t=!1,_([])},u=M=>{t=g.includes(M.key),t||_([])},w=M=>{if(t&&void 0!==M.composedPath){const r=M.composedPath().filter(p=>!!p.classList&&p.classList.contains("ion-focusable"));_(r)}},v=()=>{n.activeElement===d&&_([])};return n.addEventListener("keydown",u),n.addEventListener("focusin",w),n.addEventListener("focusout",v),n.addEventListener("touchstart",i,{passive:!0}),n.addEventListener("mousedown",i),{destroy:()=>{n.removeEventListener("keydown",u),n.removeEventListener("focusin",w),n.removeEventListener("focusout",v),n.removeEventListener("touchstart",i),n.removeEventListener("mousedown",i)},setFocus:_}}},5487:(y,h,c)=>{c.d(h,{c:()=>e});var f=c(839);const e=o=>{const t=o;let n;return{hasLegacyControl:()=>{if(void 0===n){const _=void 0!==t.label||g(t),i=t.hasAttribute("aria-label")||t.hasAttribute("aria-labelledby")&&null===t.shadowRoot,u=(0,f.h)(t);n=!0===t.legacy||!_&&!i&&null!==u}return n}}},g=o=>null!==o.shadowRoot&&!!(l.includes(o.tagName)&&null!==o.querySelector('[slot="label"]')||a.includes(o.tagName)&&""!==o.textContent),l=["ION-RANGE"],a=["ION-TOGGLE","ION-CHECKBOX","ION-RADIO"]},967:(y,h,c)=>{c.d(h,{I:()=>e,a:()=>t,b:()=>n,c:()=>o,d:()=>_,h:()=>d});var f=c(4874),e=function(i){return i.Heavy="HEAVY",i.Medium="MEDIUM",i.Light="LIGHT",i}(e||{});const l={getEngine(){const i=window.TapticEngine;if(i)return i;const u=(0,f.g)();return u?.isPluginAvailable("Haptics")?u.Plugins.Haptics:void 0},available(){if(!this.getEngine())return!1;const u=(0,f.g)();return"web"!==u?.getPlatform()||typeof navigator<"u"&&void 0!==navigator.vibrate},isCordova:()=>void 0!==window.TapticEngine,isCapacitor:()=>void 0!==(0,f.g)(),impact(i){const u=this.getEngine();if(!u)return;const w=this.isCapacitor()?i.style:i.style.toLowerCase();u.impact({style:w})},notification(i){const u=this.getEngine();if(!u)return;const w=this.isCapacitor()?i.type:i.type.toLowerCase();u.notification({type:w})},selection(){const i=this.isCapacitor()?e.Light:"light";this.impact({style:i})},selectionStart(){const i=this.getEngine();i&&(this.isCapacitor()?i.selectionStart():i.gestureSelectionStart())},selectionChanged(){const i=this.getEngine();i&&(this.isCapacitor()?i.selectionChanged():i.gestureSelectionChanged())},selectionEnd(){const i=this.getEngine();i&&(this.isCapacitor()?i.selectionEnd():i.gestureSelectionEnd())}},a=()=>l.available(),o=()=>{a()&&l.selection()},t=()=>{a()&&l.selectionStart()},n=()=>{a()&&l.selectionChanged()},d=()=>{a()&&l.selectionEnd()},_=i=>{a()&&l.impact(i)}},2874:(y,h,c)=>{c.d(h,{I:()=>o,a:()=>_,b:()=>a,c:()=>w,d:()=>E,f:()=>i,g:()=>d,i:()=>n,p:()=>v,r:()=>M,s:()=>u});var f=c(5861),e=c(839),g=c(6710);const a="ion-content",o=".ion-content-scroll-host",t=`${a}, ${o}`,n=r=>"ION-CONTENT"===r.tagName,d=function(){var r=(0,f.Z)(function*(p){return n(p)?(yield new Promise(s=>(0,e.c)(p,s)),p.getScrollElement()):p});return function(s){return r.apply(this,arguments)}}(),_=r=>r.querySelector(o)||r.querySelector(t),i=r=>r.closest(t),u=(r,p)=>n(r)?r.scrollToTop(p):Promise.resolve(r.scrollTo({top:0,left:0,behavior:p>0?"smooth":"auto"})),w=(r,p,s,O)=>n(r)?r.scrollByPoint(p,s,O):Promise.resolve(r.scrollBy({top:s,left:p,behavior:O>0?"smooth":"auto"})),v=r=>(0,g.b)(r,a),E=r=>{if(n(r)){const s=r.scrollY;return r.scrollY=!1,s}return r.style.setProperty("overflow","hidden"),!0},M=(r,p)=>{n(r)?r.scrollY=p:r.style.removeProperty("overflow")}},5307:(y,h,c)=>{c.d(h,{a:()=>f,b:()=>w,c:()=>t,d:()=>v,e:()=>A,f:()=>o,g:()=>E,h:()=>g,i:()=>e,j:()=>O,k:()=>x,l:()=>n,m:()=>i,n:()=>M,o:()=>_,p:()=>a,q:()=>l,r:()=>s,s:()=>m,t:()=>u,u:()=>r,v:()=>p,w:()=>d});const f="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path stroke-linecap='square' stroke-miterlimit='10' stroke-width='48' d='M244 400L100 256l144-144M120 256h292' class='ionicon-fill-none'/></svg>",e="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M112 268l144 144 144-144M256 392V100' class='ionicon-fill-none'/></svg>",g="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path d='M368 64L144 256l224 192V64z'/></svg>",l="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path d='M64 144l192 224 192-224H64z'/></svg>",a="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path d='M448 368L256 144 64 368h384z'/></svg>",o="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path stroke-linecap='round' stroke-linejoin='round' d='M416 128L192 384l-96-96' class='ionicon-fill-none ionicon-stroke-width'/></svg>",t="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M328 112L184 256l144 144' class='ionicon-fill-none'/></svg>",n="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M112 184l144 144 144-144' class='ionicon-fill-none'/></svg>",d="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path d='M136 208l120-104 120 104M136 304l120 104 120-104' stroke-width='48' stroke-linecap='round' stroke-linejoin='round' class='ionicon-fill-none'/></svg>",_="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M184 112l144 144-144 144' class='ionicon-fill-none'/></svg>",i="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='48' d='M184 112l144 144-144 144' class='ionicon-fill-none'/></svg>",u="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path d='M289.94 256l95-95A24 24 0 00351 127l-95 95-95-95a24 24 0 00-34 34l95 95-95 95a24 24 0 1034 34l95-95 95 95a24 24 0 0034-34z'/></svg>",w="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path d='M256 48C141.31 48 48 141.31 48 256s93.31 208 208 208 208-93.31 208-208S370.69 48 256 48zm75.31 260.69a16 16 0 11-22.62 22.62L256 278.63l-52.69 52.68a16 16 0 01-22.62-22.62L233.37 256l-52.68-52.69a16 16 0 0122.62-22.62L256 233.37l52.69-52.68a16 16 0 0122.62 22.62L278.63 256z'/></svg>",v="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path d='M400 145.49L366.51 112 256 222.51 145.49 112 112 145.49 222.51 256 112 366.51 145.49 400 256 289.49 366.51 400 400 366.51 289.49 256 400 145.49z'/></svg>",E="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><circle cx='256' cy='256' r='192' stroke-linecap='round' stroke-linejoin='round' class='ionicon-fill-none ionicon-stroke-width'/></svg>",M="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><circle cx='256' cy='256' r='48'/><circle cx='416' cy='256' r='48'/><circle cx='96' cy='256' r='48'/></svg>",r="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path stroke-linecap='round' stroke-miterlimit='10' d='M80 160h352M80 256h352M80 352h352' class='ionicon-fill-none ionicon-stroke-width'/></svg>",p="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path d='M64 384h384v-42.67H64zm0-106.67h384v-42.66H64zM64 128v42.67h384V128z'/></svg>",s="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path stroke-linecap='round' stroke-linejoin='round' d='M400 256H112' class='ionicon-fill-none ionicon-stroke-width'/></svg>",O="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path stroke-linecap='round' stroke-linejoin='round' d='M96 256h320M96 176h320M96 336h320' class='ionicon-fill-none ionicon-stroke-width'/></svg>",x="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path stroke-linecap='square' stroke-linejoin='round' stroke-width='44' d='M118 304h276M118 208h276' class='ionicon-fill-none'/></svg>",m="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path d='M221.09 64a157.09 157.09 0 10157.09 157.09A157.1 157.1 0 00221.09 64z' stroke-miterlimit='10' class='ionicon-fill-none ionicon-stroke-width'/><path stroke-linecap='round' stroke-miterlimit='10' d='M338.29 338.29L448 448' class='ionicon-fill-none ionicon-stroke-width'/></svg>",A="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path d='M464 428L339.92 303.9a160.48 160.48 0 0030.72-94.58C370.64 120.37 298.27 48 209.32 48S48 120.37 48 209.32s72.37 161.32 161.32 161.32a160.48 160.48 0 0094.58-30.72L428 464zM209.32 319.69a110.38 110.38 0 11110.37-110.37 110.5 110.5 0 01-110.37 110.37z'/></svg>"},2894:(y,h,c)=>{c.d(h,{c:()=>l,g:()=>a});var f=c(9938),e=c(839),g=c(6710);const l=(t,n,d)=>{let _,i;void 0!==f.w&&"MutationObserver"in f.w&&(_=new MutationObserver(E=>{for(const M of E)for(const r of M.addedNodes)if(r.nodeType===Node.ELEMENT_NODE&&r.slot===n)return d(),void(0,e.r)(()=>u(r))}),_.observe(t,{childList:!0}));const u=E=>{var M;i&&(i.disconnect(),i=void 0),i=new MutationObserver(r=>{d();for(const p of r)for(const s of p.removedNodes)s.nodeType===Node.ELEMENT_NODE&&s.slot===n&&v()}),i.observe(null!==(M=E.parentElement)&&void 0!==M?M:E,{subtree:!0,childList:!0})},v=()=>{i&&(i.disconnect(),i=void 0)};return{destroy:()=>{_&&(_.disconnect(),_=void 0),v()}}},a=(t,n,d)=>{const _=null==t?0:t.toString().length,i=o(_,n);if(void 0===d)return i;try{return d(_,n)}catch(u){return(0,g.a)("Exception in provided `counterFormatter`.",u),i}},o=(t,n)=>`${t} / ${n}`},7484:(y,h,c)=>{c.d(h,{K:()=>l,a:()=>g});var f=c(4874),e=function(a){return a.Unimplemented="UNIMPLEMENTED",a.Unavailable="UNAVAILABLE",a}(e||{}),g=function(a){return a.Body="body",a.Ionic="ionic",a.Native="native",a.None="none",a}(g||{});const l={getEngine(){const a=(0,f.g)();if(a?.isPluginAvailable("Keyboard"))return a.Plugins.Keyboard},getResizeMode(){const a=this.getEngine();return a?.getResizeMode?a.getResizeMode().catch(o=>{if(o.code!==e.Unimplemented)throw o}):Promise.resolve(void 0)}}},1612:(y,h,c)=>{c.r(h),c.d(h,{KEYBOARD_DID_CLOSE:()=>a,KEYBOARD_DID_OPEN:()=>l,copyVisualViewport:()=>x,keyboardDidClose:()=>r,keyboardDidOpen:()=>E,keyboardDidResize:()=>M,resetKeyboardAssist:()=>_,setKeyboardClose:()=>v,setKeyboardOpen:()=>w,startKeyboardAssist:()=>i,trackViewportChanges:()=>O});var f=c(7484);c(4874),c(9938);const l="ionKeyboardDidShow",a="ionKeyboardDidHide";let t={},n={},d=!1;const _=()=>{t={},n={},d=!1},i=m=>{if(f.K.getEngine())u(m);else{if(!m.visualViewport)return;n=x(m.visualViewport),m.visualViewport.onresize=()=>{O(m),E()||M(m)?w(m):r(m)&&v(m)}}},u=m=>{m.addEventListener("keyboardDidShow",A=>w(m,A)),m.addEventListener("keyboardDidHide",()=>v(m))},w=(m,A)=>{p(m,A),d=!0},v=m=>{s(m),d=!1},E=()=>!d&&t.width===n.width&&(t.height-n.height)*n.scale>150,M=m=>d&&!r(m),r=m=>d&&n.height===m.innerHeight,p=(m,A)=>{const D=new CustomEvent(l,{detail:{keyboardHeight:A?A.keyboardHeight:m.innerHeight-n.height}});m.dispatchEvent(D)},s=m=>{const A=new CustomEvent(a);m.dispatchEvent(A)},O=m=>{t=Object.assign({},n),n=x(m.visualViewport)},x=m=>({width:Math.round(m.width),height:Math.round(m.height),offsetTop:m.offsetTop,offsetLeft:m.offsetLeft,pageTop:m.pageTop,pageLeft:m.pageLeft,scale:m.scale})},3459:(y,h,c)=>{c.d(h,{c:()=>o});var f=c(5861),e=c(9938),g=c(7484);const l=t=>void 0===e.d||t===g.a.None||void 0===t?null:e.d.querySelector("ion-app")??e.d.body,a=t=>{const n=l(t);return null===n?0:n.clientHeight},o=function(){var t=(0,f.Z)(function*(n){let d,_,i,u;const w=function(){var p=(0,f.Z)(function*(){const s=yield g.K.getResizeMode(),O=void 0===s?void 0:s.mode;d=()=>{void 0===u&&(u=a(O)),i=!0,v(i,O)},_=()=>{i=!1,v(i,O)},null==e.w||e.w.addEventListener("keyboardWillShow",d),null==e.w||e.w.addEventListener("keyboardWillHide",_)});return function(){return p.apply(this,arguments)}}(),v=(p,s)=>{n&&n(p,E(s))},E=p=>{if(0===u||u===a(p))return;const s=l(p);return null!==s?new Promise(O=>{const m=new ResizeObserver(()=>{s.clientHeight===u&&(m.disconnect(),O())});m.observe(s)}):void 0};return yield w(),{init:w,destroy:()=>{null==e.w||e.w.removeEventListener("keyboardWillShow",d),null==e.w||e.w.removeEventListener("keyboardWillHide",_),d=_=void 0},isKeyboardVisible:()=>i}});return function(d){return t.apply(this,arguments)}}()},3830:(y,h,c)=>{c.d(h,{c:()=>e});var f=c(5861);const e=()=>{let g;return{lock:function(){var a=(0,f.Z)(function*(){const o=g;let t;return g=new Promise(n=>t=n),void 0!==o&&(yield o),t});return function(){return a.apply(this,arguments)}}()}}},5857:(y,h,c)=>{c.d(h,{c:()=>g});var f=c(9938),e=c(839);const g=(l,a,o)=>{let t;const n=()=>!(void 0===a()||void 0!==l.label||null===o()),_=()=>{const u=a();if(void 0===u)return;if(!n())return void u.style.removeProperty("width");const w=o().scrollWidth;if(0===w&&null===u.offsetParent&&void 0!==f.w&&"IntersectionObserver"in f.w){if(void 0!==t)return;const v=t=new IntersectionObserver(E=>{1===E[0].intersectionRatio&&(_(),v.disconnect(),t=void 0)},{threshold:.01,root:l});v.observe(u)}else u.style.setProperty("width",.75*w+"px")};return{calculateNotchWidth:()=>{n()&&(0,e.r)(()=>{_()})},destroy:()=>{t&&(t.disconnect(),t=void 0)}}}},3781:(y,h,c)=>{c.d(h,{S:()=>e});const e={bubbles:{dur:1e3,circles:9,fn:(g,l,a)=>{const o=g*l/a-g+"ms",t=2*Math.PI*l/a;return{r:5,style:{top:32*Math.sin(t)+"%",left:32*Math.cos(t)+"%","animation-delay":o}}}},circles:{dur:1e3,circles:8,fn:(g,l,a)=>{const o=l/a,t=g*o-g+"ms",n=2*Math.PI*o;return{r:5,style:{top:32*Math.sin(n)+"%",left:32*Math.cos(n)+"%","animation-delay":t}}}},circular:{dur:1400,elmDuration:!0,circles:1,fn:()=>({r:20,cx:48,cy:48,fill:"none",viewBox:"24 24 48 48",transform:"translate(0,0)",style:{}})},crescent:{dur:750,circles:1,fn:()=>({r:26,style:{}})},dots:{dur:750,circles:3,fn:(g,l)=>({r:6,style:{left:32-32*l+"%","animation-delay":-110*l+"ms"}})},lines:{dur:1e3,lines:8,fn:(g,l,a)=>({y1:14,y2:26,style:{transform:`rotate(${360/a*l+(l<a/2?180:-180)}deg)`,"animation-delay":g*l/a-g+"ms"}})},"lines-small":{dur:1e3,lines:8,fn:(g,l,a)=>({y1:12,y2:20,style:{transform:`rotate(${360/a*l+(l<a/2?180:-180)}deg)`,"animation-delay":g*l/a-g+"ms"}})},"lines-sharp":{dur:1e3,lines:12,fn:(g,l,a)=>({y1:17,y2:29,style:{transform:`rotate(${30*l+(l<6?180:-180)}deg)`,"animation-delay":g*l/a-g+"ms"}})},"lines-sharp-small":{dur:1e3,lines:12,fn:(g,l,a)=>({y1:12,y2:20,style:{transform:`rotate(${30*l+(l<6?180:-180)}deg)`,"animation-delay":g*l/a-g+"ms"}})}}},8663:(y,h,c)=>{c.r(h),c.d(h,{createSwipeBackGesture:()=>a});var f=c(839),e=c(5085),g=c(9203);c(619);const a=(o,t,n,d,_)=>{const i=o.ownerDocument.defaultView;let u=(0,e.i)(o);const v=s=>u?-s.deltaX:s.deltaX;return(0,g.createGesture)({el:o,gestureName:"goback-swipe",gesturePriority:101,threshold:10,canStart:s=>(u=(0,e.i)(o),(s=>{const{startX:x}=s;return u?x>=i.innerWidth-50:x<=50})(s)&&t()),onStart:n,onMove:s=>{const x=v(s)/i.innerWidth;d(x)},onEnd:s=>{const O=v(s),x=i.innerWidth,m=O/x,A=(s=>u?-s.velocityX:s.velocityX)(s),D=A>=0&&(A>.2||O>x/2),L=(D?1-m:m)*x;let T=0;if(L>5){const b=L/Math.abs(A);T=Math.min(b,540)}_(D,m<=0?.01:(0,f.l)(0,m,.9999),T)}})}},5564:(y,h,c)=>{c.d(h,{w:()=>f});const f=(l,a,o)=>{if(typeof MutationObserver>"u")return;const t=new MutationObserver(n=>{o(e(n,a))});return t.observe(l,{childList:!0,subtree:!0}),t},e=(l,a)=>{let o;return l.forEach(t=>{for(let n=0;n<t.addedNodes.length;n++)o=g(t.addedNodes[n],a)||o}),o},g=(l,a)=>{if(1!==l.nodeType)return;const o=l;return(o.tagName===a.toUpperCase()?[o]:Array.from(o.querySelectorAll(a))).find(n=>n.value===o.value)}},3573:(y,h,c)=>{c.d(h,{K:()=>a});var f=c(6814),e=c(95),g=c(3582),l=c(2029);let a=(()=>{class o{static \u0275fac=function(d){return new(d||o)};static \u0275mod=l.oAB({type:o});static \u0275inj=l.cJS({imports:[f.ez,e.u5,g.Pc]})}return o})()},2565:(y,h,c)=>{c.d(h,{G:()=>M,X:()=>E});var f=c(76),e=c(2029),g=c(6814),l=c(3582);function a(r,p){if(1&r&&(e.TgZ(0,"ion-col",3),e._UZ(1,"ion-icon",4),e.TgZ(2,"div",5),e._uU(3),e.qZA(),e.TgZ(4,"div",6),e._uU(5),e.qZA()()),2&r){const s=e.oxw();e.xp6(1),e.MGl("src","assets/imgs/",s.gearImage,""),e.xp6(2),e.Oqu(s.titlestart),e.xp6(2),e.Oqu(s.titlerest)}}function o(r,p){if(1&r&&(e.TgZ(0,"ion-col",3)(1,"div",7),e._uU(2),e.qZA(),e.TgZ(3,"div",8),e._uU(4),e.qZA()()),2&r){const s=e.oxw();e.xp6(2),e.Oqu(s.titlestart),e.xp6(2),e.Oqu(s.titlerest)}}function t(r,p){if(1&r&&(e.TgZ(0,"ion-note",12)(1,"span",13),e._uU(2),e.ALo(3,"number"),e._UZ(4,"br"),e._uU(5),e.ALo(6,"number"),e._UZ(7,"br"),e.TgZ(8,"span",14),e._uU(9),e.ALo(10,"number"),e.qZA()()()),2&r){const s=e.oxw(2);e.xp6(2),e.AsE("\xa0\xa0\xa0",s.dNoteLabel,": ",e.xi3(3,5,s.item.wertung.noteD,"2.3-3")," "),e.xp6(3),e.AsE("\xa0\xa0\xa0",s.eNoteLabel,": ",e.xi3(6,8,s.item.wertung.noteE,"2.3-3")," "),e.xp6(4),e.Oqu(e.xi3(10,11,s.item.wertung.endnote,"2.3-3"))}}function n(r,p){if(1&r&&(e.TgZ(0,"ion-note",15)(1,"span",13),e._UZ(2,"br"),e._uU(3),e.ALo(4,"number"),e._UZ(5,"br"),e.TgZ(6,"span",16),e._uU(7),e.ALo(8,"number"),e.qZA()()()),2&r){const s=e.oxw(2);e.xp6(3),e.AsE("\xa0\xa0\xa0",s.eNoteLabel,": ",e.xi3(4,3,s.item.wertung.noteE,"2.3-3")," "),e.xp6(4),e.Oqu(e.xi3(8,6,s.item.wertung.endnote,"2.3-3"))}}function d(r,p){if(1&r&&(e.TgZ(0,"ion-note",15)(1,"span",16),e._uU(2),e.ALo(3,"number"),e.qZA()()),2&r){const s=e.oxw(2);e.xp6(2),e.Oqu(e.xi3(3,1,s.item.wertung.endnote,"2.3-3"))}}function _(r,p){if(1&r&&(e.TgZ(0,"ion-col",9),e.YNc(1,t,11,14,"ion-note",10),e.YNc(2,n,9,9,"ion-note",11),e.YNc(3,d,4,4,"ion-note",11),e.qZA()),2&r){const s=e.oxw();e.xp6(1),e.Q6J("ngIf",s.item.isDNoteUsed&&void 0!==s.item.wertung.noteD),e.xp6(1),e.Q6J("ngIf",!s.item.isDNoteUsed&&s.item.wertung.noteE!==s.item.wertung.endnote),e.xp6(1),e.Q6J("ngIf",!s.item.isDNoteUsed&&s.item.wertung.noteE===s.item.wertung.endnote)}}function i(r,p){if(1&r&&(e.TgZ(0,"ion-note",15)(1,"span",13),e._uU(2),e.ALo(3,"number"),e._UZ(4,"br"),e._uU(5),e.ALo(6,"number"),e._UZ(7,"br"),e.qZA(),e.TgZ(8,"span",16),e._uU(9),e.ALo(10,"number"),e.qZA()()),2&r){const s=e.oxw(2);e.xp6(2),e.AsE("",s.dNoteLabel,": ",e.xi3(3,5,s.item.wertung.noteD,"2.3-3"),"\xa0\xa0 "),e.xp6(3),e.AsE("",s.eNoteLabel,": ",e.xi3(6,8,s.item.wertung.noteE,"2.3-3"),"\xa0\xa0 "),e.xp6(4),e.hij("",e.xi3(10,11,s.item.wertung.endnote,"2.3-3"),"\xa0\xa0")}}function u(r,p){if(1&r&&(e.TgZ(0,"ion-note",15),e._UZ(1,"br"),e._uU(2),e.ALo(3,"number"),e._UZ(4,"br"),e.TgZ(5,"span",16),e._uU(6),e.ALo(7,"number"),e.qZA()()),2&r){const s=e.oxw(2);e.xp6(2),e.AsE("",s.eNoteLabel,": ",e.xi3(3,3,s.item.wertung.noteE,"2.3-3"),"\xa0\xa0 "),e.xp6(4),e.hij("",e.xi3(7,6,s.item.wertung.endnote,"2.3-3"),"\xa0\xa0")}}function w(r,p){if(1&r&&(e.TgZ(0,"ion-note",15)(1,"span",16),e._uU(2),e.ALo(3,"number"),e.qZA()()),2&r){const s=e.oxw(2);e.xp6(2),e.hij("",e.xi3(3,1,s.item.wertung.endnote,"2.3-3"),"\xa0\xa0")}}function v(r,p){if(1&r&&(e.TgZ(0,"ion-row")(1,"ion-col",17)(2,"div",18)(3,"div",19),e._uU(4),e.qZA()(),e.TgZ(5,"div",18),e._uU(6),e.qZA()(),e.TgZ(7,"ion-col",20),e.YNc(8,i,11,14,"ion-note",11),e.YNc(9,u,8,9,"ion-note",11),e.YNc(10,w,4,4,"ion-note",11),e.qZA()()),2&r){const s=e.oxw();e.xp6(4),e.Oqu(s.item.vorname+" "+s.item.name),e.xp6(2),e.hij(" ",s.item.verein," "),e.xp6(2),e.Q6J("ngIf",s.item.isDNoteUsed&&void 0!==s.item.wertung.noteD),e.xp6(1),e.Q6J("ngIf",!s.item.isDNoteUsed&&s.item.wertung.noteE!==s.item.wertung.endnote),e.xp6(1),e.Q6J("ngIf",!s.item.isDNoteUsed&&s.item.wertung.noteE===s.item.wertung.endnote)}}var E=function(r){return r[r.NONE=0]="NONE",r[r.ATHLET=1]="ATHLET",r[r.PROGRAMM=2]="PROGRAMM",r}(E||{});let M=(()=>{class r{groupBy=E;item;title;groupedBy=E.NONE;get dNoteLabel(){return this.item.isDNoteUsed&&f.TA.indexOf(this.item.programm)>-1?"A":"D"}get eNoteLabel(){return this.item.isDNoteUsed&&f.TA.indexOf(this.item.programm)>-1?"B":"E"}get titlestart(){return this.title.split(" - ")[0]}get titlerest(){return this.title.split(" - ")[1]}get gearImage(){return f.WZ[this.item.geraet]||void 0}constructor(){}ngOnInit(){}static \u0275fac=function(O){return new(O||r)};static \u0275cmp=e.Xpm({type:r,selectors:[["result-display"]],inputs:{item:"item",title:"title",groupedBy:"groupedBy"},decls:7,vars:4,consts:[["align-self-top","","class","nonwrapping","size-xs","8","size-md","8","size-lg","8","size-xl","8",4,"ngIf"],["col-auto","","align-self-end","",4,"ngIf"],[4,"ngIf"],["align-self-top","","size-xs","8","size-md","8","size-lg","8","size-xl","8",1,"nonwrapping"],[1,"img-wrapper",3,"src"],[1,"after-icon","fixedheight",2,"font-size","18pt"],[1,"after-icon","fixedheight",2,"font-size","12pt"],[2,"font-size","18pt"],[2,"font-size","12pt"],["col-auto","","align-self-end",""],["item-text-wrap","",4,"ngIf"],["item-end","","item-text-wrap","",4,"ngIf"],["item-text-wrap",""],[1,"rightbound"],[2,"font-size","18pt","font-weight","bold"],["item-end","","item-text-wrap",""],[1,"rightbound",2,"font-size","18pt","font-weight","bold"],["size-xs","8","size-md","8","size-lg","8","size-xl","8"],[1,"nonwrapping"],[2,"font-size","16pt"],["col-auto",""]],template:function(O,x){1&O&&(e.TgZ(0,"ion-card")(1,"ion-grid")(2,"ion-row"),e.YNc(3,a,6,3,"ion-col",0),e.YNc(4,o,5,2,"ion-col",0),e.YNc(5,_,4,3,"ion-col",1),e.qZA(),e.YNc(6,v,11,5,"ion-row",2),e.qZA()()),2&O&&(e.xp6(3),e.Q6J("ngIf",x.gearImage),e.xp6(1),e.Q6J("ngIf",!x.gearImage),e.xp6(1),e.Q6J("ngIf",x.groupedBy===x.groupBy.ATHLET),e.xp6(1),e.Q6J("ngIf",x.groupedBy!==x.groupBy.ATHLET))},dependencies:[g.O5,l.PM,l.wI,l.jY,l.gu,l.uN,l.Nd,g.JJ],styles:["ion-card[_ngcontent-%COMP%]{margin:auto;position:relative;color:var(--ion-card-color);background-color:var(--ion-card-background-color);border-radius:4px;box-shadow:0 3px 1px -2px var(--ion-card-shadow-color1),0 2px 2px 0 var(--ion-card-shadow-color2),0 1px 5px 0 var(--ion-card-shadow-color3)}ion-icon[_ngcontent-%COMP%]{color:var(--ion-card-color);font-size:50px;border-color:var(--ion-item-border-color);border-width:2px;border-style:solid;border-radius:4px}.fixedheight[_ngcontent-%COMP%]{word-wrap:normal;position:relative}.img-wrapper[_ngcontent-%COMP%]{position:absolute;left:0;top:4px}.after-icon[_ngcontent-%COMP%]{left:52px}.rightbound[_ngcontent-%COMP%]{float:right}.nonwrapping[_ngcontent-%COMP%]{width:100%;display:inline-block;white-space:nowrap}"]})}return r})()},3620:(y,h,c)=>{c.d(h,{b:()=>l});var f=c(4352),e=c(9360),g=c(8251);function l(a,o=f.z){return(0,e.e)((t,n)=>{let d=null,_=null,i=null;const u=()=>{if(d){d.unsubscribe(),d=null;const v=_;_=null,n.next(v)}};function w(){const v=i+a,E=o.now();if(E<v)return d=this.schedule(void 0,v-E),void n.add(d);u()}t.subscribe((0,g.x)(n,v=>{_=v,i=o.now(),d||(d=o.schedule(w,a),n.add(d))},()=>{u(),n.complete()},void 0,()=>{_=d=null}))})}}}]);