import{a as z}from"./chunk-C6TWYN5N.js";import{a as H,b as w,d as O,i as q}from"./chunk-KXBQQBPP.js";import{a as T,c as Y}from"./chunk-LOHYXAXQ.js";import{a as _}from"./chunk-HC6MZPB3.js";import{b as D,c as M}from"./chunk-IFNCDCK6.js";import{a as I,f as L}from"./chunk-UJQTAKBF.js";import"./chunk-WMEG6PAA.js";import{a as g}from"./chunk-426OJ4HC.js";import"./chunk-MM5QLNJM.js";import{a as S,b as E,h as R,i as b,m as v}from"./chunk-H3GX5QFY.js";import"./chunk-MCRJI3T3.js";import{a as y,c as m}from"./chunk-TI7J2N3O.js";import{b as C,d as N,e as c,g as a,h as P,k as A,l as x}from"./chunk-TTFYQ64X.js";import{g as f}from"./chunk-2R6CW7ES.js";var U=t=>{let e=t.previousElementSibling;return e!==null&&e.tagName==="ION-HEADER"?"translate":"scale"},G=(t,e,r)=>t==="scale"?j(e,r):V(e,r),X=t=>{let e=t.querySelector("ion-spinner"),r=e.shadowRoot.querySelector("circle"),s=t.querySelector(".spinner-arrow-container"),n=t.querySelector(".arrow-container"),l=n?n.querySelector("ion-icon"):null,o=g().duration(1e3).easing("ease-out"),i=g().addElement(s).keyframes([{offset:0,opacity:"0.3"},{offset:.45,opacity:"0.3"},{offset:.55,opacity:"1"},{offset:1,opacity:"1"}]),h=g().addElement(r).keyframes([{offset:0,strokeDasharray:"1px, 200px"},{offset:.2,strokeDasharray:"1px, 200px"},{offset:.55,strokeDasharray:"100px, 200px"},{offset:1,strokeDasharray:"100px, 200px"}]),d=g().addElement(e).keyframes([{offset:0,transform:"rotate(-90deg)"},{offset:1,transform:"rotate(210deg)"}]);if(n&&l){let u=g().addElement(n).keyframes([{offset:0,transform:"rotate(0deg)"},{offset:.3,transform:"rotate(0deg)"},{offset:.55,transform:"rotate(280deg)"},{offset:1,transform:"rotate(400deg)"}]),p=g().addElement(l).keyframes([{offset:0,transform:"translateX(2px) scale(0)"},{offset:.3,transform:"translateX(2px) scale(0)"},{offset:.55,transform:"translateX(-1.5px) scale(1)"},{offset:1,transform:"translateX(-1.5px) scale(1)"}]);o.addAnimation([u,p])}return o.addAnimation([i,h,d])},j=(t,e)=>{let r=e.clientHeight,s=g().addElement(t).keyframes([{offset:0,transform:`scale(0) translateY(-${r}px)`},{offset:1,transform:"scale(1) translateY(100px)"}]);return X(t).addAnimation([s])},V=(t,e)=>{let r=e.clientHeight,s=g().addElement(t).keyframes([{offset:0,transform:`translateY(-${r}px)`},{offset:1,transform:"translateY(100px)"}]);return X(t).addAnimation([s])},W=t=>g().duration(125).addElement(t).fromTo("transform","translateY(var(--ion-pulling-refresher-translate, 100px))","translateY(0px)"),K=(t,e)=>{t.style.setProperty("opacity",e.toString())},Z=(t,e,r)=>{c(()=>{t.forEach((n,l)=>{let o=l*(1/e),i=1-o,h=r-o,d=v(0,h/i,1);n.style.setProperty("opacity",d.toString())})})},J=(t,e)=>{c(()=>{t.style.setProperty("--refreshing-rotation-duration",e>=1?"0.5s":"2s"),t.style.setProperty("opacity","1")})},k=(t,e,r=200)=>{if(!t)return Promise.resolve();let s=S(t,r);return c(()=>{t.style.setProperty("transition",`${r}ms all ease-out`),e===void 0?t.style.removeProperty("transform"):t.style.setProperty("transform",`translate3d(0px, ${e}, 0px)`)}),s},$=()=>navigator.maxTouchPoints>0&&CSS.supports("background: -webkit-named-image(apple-pay-logo-black)"),F=(t,e)=>f(void 0,null,function*(){let r=t.querySelector("ion-refresher-content");if(!r)return Promise.resolve(!1);yield new Promise(l=>E(r,l));let s=t.querySelector("ion-refresher-content .refresher-pulling ion-spinner"),n=t.querySelector("ion-refresher-content .refresher-refreshing ion-spinner");return s!==null&&n!==null&&(e==="ios"&&$()||e==="md")}),Q="ion-refresher{top:0;display:none;position:absolute;width:100%;height:60px;pointer-events:none;z-index:-1}ion-refresher{inset-inline-start:0}ion-refresher.refresher-active{display:block}ion-refresher-content{display:-ms-flexbox;display:flex;-ms-flex-direction:column;flex-direction:column;-ms-flex-pack:center;justify-content:center;height:100%}.refresher-pulling,.refresher-refreshing{display:none;width:100%}.refresher-pulling-icon,.refresher-refreshing-icon{-webkit-transform-origin:center;transform-origin:center;-webkit-transition:200ms;transition:200ms;font-size:30px;text-align:center}:host-context([dir=rtl]) .refresher-pulling-icon,:host-context([dir=rtl]) .refresher-refreshing-icon{-webkit-transform-origin:calc(100% - center);transform-origin:calc(100% - center)}[dir=rtl] .refresher-pulling-icon,[dir=rtl] .refresher-refreshing-icon{-webkit-transform-origin:calc(100% - center);transform-origin:calc(100% - center)}@supports selector(:dir(rtl)){.refresher-pulling-icon:dir(rtl),.refresher-refreshing-icon:dir(rtl){-webkit-transform-origin:calc(100% - center);transform-origin:calc(100% - center)}}.refresher-pulling-text,.refresher-refreshing-text{font-size:16px;text-align:center}ion-refresher-content .arrow-container{display:none}.refresher-pulling ion-refresher-content .refresher-pulling{display:block}.refresher-ready ion-refresher-content .refresher-pulling{display:block}.refresher-ready ion-refresher-content .refresher-pulling-icon{-webkit-transform:rotate(180deg);transform:rotate(180deg)}.refresher-refreshing ion-refresher-content .refresher-refreshing{display:block}.refresher-cancelling ion-refresher-content .refresher-pulling{display:block}.refresher-cancelling ion-refresher-content .refresher-pulling-icon{-webkit-transform:scale(0);transform:scale(0)}.refresher-completing ion-refresher-content .refresher-refreshing{display:block}.refresher-completing ion-refresher-content .refresher-refreshing-icon{-webkit-transform:scale(0);transform:scale(0)}.refresher-native .refresher-pulling-text,.refresher-native .refresher-refreshing-text{display:none}.refresher-ios .refresher-pulling-icon,.refresher-ios .refresher-refreshing-icon{color:var(--ion-text-color, #000)}.refresher-ios .refresher-pulling-text,.refresher-ios .refresher-refreshing-text{color:var(--ion-text-color, #000)}.refresher-ios .refresher-refreshing .spinner-lines-ios line,.refresher-ios .refresher-refreshing .spinner-lines-small-ios line,.refresher-ios .refresher-refreshing .spinner-crescent circle{stroke:var(--ion-text-color, #000)}.refresher-ios .refresher-refreshing .spinner-bubbles circle,.refresher-ios .refresher-refreshing .spinner-circles circle,.refresher-ios .refresher-refreshing .spinner-dots circle{fill:var(--ion-text-color, #000)}ion-refresher.refresher-native{display:block;z-index:1}ion-refresher.refresher-native ion-spinner{-webkit-margin-start:auto;margin-inline-start:auto;-webkit-margin-end:auto;margin-inline-end:auto;margin-top:0;margin-bottom:0}.refresher-native .refresher-refreshing ion-spinner{--refreshing-rotation-duration:2s;display:none;-webkit-animation:var(--refreshing-rotation-duration) ease-out refresher-rotate forwards;animation:var(--refreshing-rotation-duration) ease-out refresher-rotate forwards}.refresher-native .refresher-refreshing{display:none;-webkit-animation:250ms linear refresher-pop forwards;animation:250ms linear refresher-pop forwards}.refresher-native ion-spinner{width:32px;height:32px;color:var(--ion-color-step-450, var(--ion-background-color-step-450, #747577))}.refresher-native.refresher-refreshing .refresher-pulling ion-spinner,.refresher-native.refresher-completing .refresher-pulling ion-spinner{display:none}.refresher-native.refresher-refreshing .refresher-refreshing ion-spinner,.refresher-native.refresher-completing .refresher-refreshing ion-spinner{display:block}.refresher-native.refresher-pulling .refresher-pulling ion-spinner{display:block}.refresher-native.refresher-pulling .refresher-refreshing ion-spinner{display:none}.refresher-native.refresher-completing ion-refresher-content .refresher-refreshing-icon{-webkit-transform:scale(0) rotate(180deg);transform:scale(0) rotate(180deg);-webkit-transition:300ms;transition:300ms}@-webkit-keyframes refresher-pop{0%{-webkit-transform:scale(1);transform:scale(1);-webkit-animation-timing-function:ease-in;animation-timing-function:ease-in}50%{-webkit-transform:scale(1.2);transform:scale(1.2);-webkit-animation-timing-function:ease-out;animation-timing-function:ease-out}100%{-webkit-transform:scale(1);transform:scale(1)}}@keyframes refresher-pop{0%{-webkit-transform:scale(1);transform:scale(1);-webkit-animation-timing-function:ease-in;animation-timing-function:ease-in}50%{-webkit-transform:scale(1.2);transform:scale(1.2);-webkit-animation-timing-function:ease-out;animation-timing-function:ease-out}100%{-webkit-transform:scale(1);transform:scale(1)}}@-webkit-keyframes refresher-rotate{from{-webkit-transform:rotate(0deg);transform:rotate(0deg)}to{-webkit-transform:rotate(180deg);transform:rotate(180deg)}}@keyframes refresher-rotate{from{-webkit-transform:rotate(0deg);transform:rotate(0deg)}to{-webkit-transform:rotate(180deg);transform:rotate(180deg)}}",ee=Q,re="ion-refresher{top:0;display:none;position:absolute;width:100%;height:60px;pointer-events:none;z-index:-1}ion-refresher{inset-inline-start:0}ion-refresher.refresher-active{display:block}ion-refresher-content{display:-ms-flexbox;display:flex;-ms-flex-direction:column;flex-direction:column;-ms-flex-pack:center;justify-content:center;height:100%}.refresher-pulling,.refresher-refreshing{display:none;width:100%}.refresher-pulling-icon,.refresher-refreshing-icon{-webkit-transform-origin:center;transform-origin:center;-webkit-transition:200ms;transition:200ms;font-size:30px;text-align:center}:host-context([dir=rtl]) .refresher-pulling-icon,:host-context([dir=rtl]) .refresher-refreshing-icon{-webkit-transform-origin:calc(100% - center);transform-origin:calc(100% - center)}[dir=rtl] .refresher-pulling-icon,[dir=rtl] .refresher-refreshing-icon{-webkit-transform-origin:calc(100% - center);transform-origin:calc(100% - center)}@supports selector(:dir(rtl)){.refresher-pulling-icon:dir(rtl),.refresher-refreshing-icon:dir(rtl){-webkit-transform-origin:calc(100% - center);transform-origin:calc(100% - center)}}.refresher-pulling-text,.refresher-refreshing-text{font-size:16px;text-align:center}ion-refresher-content .arrow-container{display:none}.refresher-pulling ion-refresher-content .refresher-pulling{display:block}.refresher-ready ion-refresher-content .refresher-pulling{display:block}.refresher-ready ion-refresher-content .refresher-pulling-icon{-webkit-transform:rotate(180deg);transform:rotate(180deg)}.refresher-refreshing ion-refresher-content .refresher-refreshing{display:block}.refresher-cancelling ion-refresher-content .refresher-pulling{display:block}.refresher-cancelling ion-refresher-content .refresher-pulling-icon{-webkit-transform:scale(0);transform:scale(0)}.refresher-completing ion-refresher-content .refresher-refreshing{display:block}.refresher-completing ion-refresher-content .refresher-refreshing-icon{-webkit-transform:scale(0);transform:scale(0)}.refresher-native .refresher-pulling-text,.refresher-native .refresher-refreshing-text{display:none}.refresher-md .refresher-pulling-icon,.refresher-md .refresher-refreshing-icon{color:var(--ion-text-color, #000)}.refresher-md .refresher-pulling-text,.refresher-md .refresher-refreshing-text{color:var(--ion-text-color, #000)}.refresher-md .refresher-refreshing .spinner-lines-md line,.refresher-md .refresher-refreshing .spinner-lines-small-md line,.refresher-md .refresher-refreshing .spinner-crescent circle{stroke:var(--ion-text-color, #000)}.refresher-md .refresher-refreshing .spinner-bubbles circle,.refresher-md .refresher-refreshing .spinner-circles circle,.refresher-md .refresher-refreshing .spinner-dots circle{fill:var(--ion-text-color, #000)}ion-refresher.refresher-native{display:block;z-index:1}ion-refresher.refresher-native ion-spinner{-webkit-margin-start:auto;margin-inline-start:auto;-webkit-margin-end:auto;margin-inline-end:auto;margin-top:0;margin-bottom:0;width:24px;height:24px;color:var(--ion-color-primary, #0054e9)}ion-refresher.refresher-native .spinner-arrow-container{display:inherit}ion-refresher.refresher-native .arrow-container{display:block;position:absolute;width:24px;height:24px}ion-refresher.refresher-native .arrow-container ion-icon{-webkit-margin-start:auto;margin-inline-start:auto;-webkit-margin-end:auto;margin-inline-end:auto;margin-top:0;margin-bottom:0;left:0;right:0;bottom:-4px;position:absolute;color:var(--ion-color-primary, #0054e9);font-size:12px}ion-refresher.refresher-native.refresher-pulling ion-refresher-content .refresher-pulling,ion-refresher.refresher-native.refresher-ready ion-refresher-content .refresher-pulling{display:-ms-flexbox;display:flex}ion-refresher.refresher-native.refresher-refreshing ion-refresher-content .refresher-refreshing,ion-refresher.refresher-native.refresher-completing ion-refresher-content .refresher-refreshing,ion-refresher.refresher-native.refresher-cancelling ion-refresher-content .refresher-refreshing{display:-ms-flexbox;display:flex}ion-refresher.refresher-native .refresher-pulling-icon{-webkit-transform:translateY(calc(-100% - 10px));transform:translateY(calc(-100% - 10px))}ion-refresher.refresher-native .refresher-pulling-icon,ion-refresher.refresher-native .refresher-refreshing-icon{-webkit-margin-start:auto;margin-inline-start:auto;-webkit-margin-end:auto;margin-inline-end:auto;margin-top:0;margin-bottom:0;border-radius:100%;-webkit-padding-start:8px;padding-inline-start:8px;-webkit-padding-end:8px;padding-inline-end:8px;padding-top:8px;padding-bottom:8px;display:-ms-flexbox;display:flex;border:1px solid var(--ion-color-step-200, var(--ion-background-color-step-200, #ececec));background:var(--ion-color-step-250, var(--ion-background-color-step-250, #ffffff));-webkit-box-shadow:0px 1px 6px rgba(0, 0, 0, 0.1);box-shadow:0px 1px 6px rgba(0, 0, 0, 0.1)}",te=re,ue=(()=>{let t=class{constructor(e){C(this,e),this.ionRefresh=x(this,"ionRefresh",7),this.ionPull=x(this,"ionPull",7),this.ionStart=x(this,"ionStart",7),this.appliedStyles=!1,this.didStart=!1,this.progress=0,this.pointerDown=!1,this.needsCompletion=!1,this.didRefresh=!1,this.contentFullscreen=!1,this.lastVelocityY=0,this.animations=[],this.nativeRefresher=!1,this.state=1,this.pullMin=60,this.pullMax=this.pullMin+60,this.closeDuration="280ms",this.snapbackDuration="280ms",this.pullFactor=1,this.disabled=!1}disabledChanged(){this.gesture&&this.gesture.enable(!this.disabled)}checkNativeRefresher(){return f(this,null,function*(){let e=yield F(this.el,m(this));if(e&&!this.nativeRefresher){let r=this.el.closest("ion-content");this.setupNativeRefresher(r)}else e||this.destroyNativeRefresher()})}destroyNativeRefresher(){this.scrollEl&&this.scrollListenerCallback&&(this.scrollEl.removeEventListener("scroll",this.scrollListenerCallback),this.scrollListenerCallback=void 0),this.nativeRefresher=!1}resetNativeRefresher(e,r){return f(this,null,function*(){this.state=r,m(this)==="ios"?yield k(e,void 0,300):yield S(this.el.querySelector(".refresher-refreshing-icon"),200),this.didRefresh=!1,this.needsCompletion=!1,this.pointerDown=!1,this.animations.forEach(s=>s.destroy()),this.animations=[],this.progress=0,this.state=1})}setupiOSNativeRefresher(e,r){return f(this,null,function*(){this.elementToTransform=this.scrollEl;let s=e.shadowRoot.querySelectorAll("svg"),n=this.scrollEl.clientHeight*.16,l=s.length;c(()=>s.forEach(o=>o.style.setProperty("animation","none"))),this.scrollListenerCallback=()=>{!this.pointerDown&&this.state===1||N(()=>{let o=this.scrollEl.scrollTop,i=this.el.clientHeight;if(o>0){if(this.state===8){let p=v(0,o/(i*.5),1);c(()=>K(r,1-p));return}return}this.pointerDown&&(this.didStart||(this.didStart=!0,this.ionStart.emit()),this.pointerDown&&this.ionPull.emit());let h=this.didStart?30:0,d=this.progress=v(0,(Math.abs(o)-h)/n,1);this.state===8||d===1?(this.pointerDown&&J(r,this.lastVelocityY),this.didRefresh||(this.beginRefresh(),this.didRefresh=!0,L({style:I.Light}),this.pointerDown||k(this.elementToTransform,`${i}px`))):(this.state=2,Z(s,l,d))})},this.scrollEl.addEventListener("scroll",this.scrollListenerCallback),this.gesture=(yield import("./chunk-F5F7W64E.js")).createGesture({el:this.scrollEl,gestureName:"refresher",gesturePriority:31,direction:"y",threshold:5,onStart:()=>{this.pointerDown=!0,this.didRefresh||k(this.elementToTransform,"0px"),n===0&&(n=this.scrollEl.clientHeight*.16)},onMove:o=>{this.lastVelocityY=o.velocityY},onEnd:()=>{this.pointerDown=!1,this.didStart=!1,this.needsCompletion?(this.resetNativeRefresher(this.elementToTransform,32),this.needsCompletion=!1):this.didRefresh&&N(()=>k(this.elementToTransform,`${this.el.clientHeight}px`))}}),this.disabledChanged()})}setupMDNativeRefresher(e,r,s){return f(this,null,function*(){let n=R(r).querySelector("circle"),l=this.el.querySelector("ion-refresher-content .refresher-pulling-icon"),o=R(s).querySelector("circle");n!==null&&o!==null&&c(()=>{n.style.setProperty("animation","none"),s.style.setProperty("animation-delay","-655ms"),o.style.setProperty("animation-delay","-655ms")}),this.gesture=(yield import("./chunk-F5F7W64E.js")).createGesture({el:this.scrollEl,gestureName:"refresher",gesturePriority:31,direction:"y",threshold:5,canStart:()=>this.state!==8&&this.state!==32&&this.scrollEl.scrollTop===0,onStart:i=>{this.progress=0,i.data={animation:void 0,didStart:!1,cancelled:!1}},onMove:i=>{if(i.velocityY<0&&this.progress===0&&!i.data.didStart||i.data.cancelled){i.data.cancelled=!0;return}if(!i.data.didStart){i.data.didStart=!0,this.state=2;let{scrollEl:h}=this,d=h.matches(w)?"overflow":"--overflow";c(()=>h.style.setProperty(d,"hidden"));let u=U(e),p=G(u,l,this.el);i.data.animation=p,p.progressStart(!1,0),this.ionStart.emit(),this.animations.push(p);return}this.progress=v(0,i.deltaY/180*.5,1),i.data.animation.progressStep(this.progress),this.ionPull.emit()},onEnd:i=>{if(!i.data.didStart)return;this.gesture.enable(!1);let{scrollEl:h}=this,d=h.matches(w)?"overflow":"--overflow";if(c(()=>h.style.removeProperty(d)),this.progress<=.4){i.data.animation.progressEnd(0,this.progress,500).onFinish(()=>{this.animations.forEach(B=>B.destroy()),this.animations=[],this.gesture.enable(!0),this.state=1});return}let u=_([0,0],[0,0],[1,1],[1,1],this.progress)[0],p=W(l);this.animations.push(p),c(()=>f(this,null,function*(){l.style.setProperty("--ion-pulling-refresher-translate",`${u*100}px`),i.data.animation.progressEnd(),yield p.play(),this.beginRefresh(),i.data.animation.destroy(),this.gesture.enable(!0)}))}}),this.disabledChanged()})}setupNativeRefresher(e){return f(this,null,function*(){if(this.scrollListenerCallback||!e||this.nativeRefresher||!this.scrollEl)return;this.setCss(0,"",!1,""),this.nativeRefresher=!0;let r=this.el.querySelector("ion-refresher-content .refresher-pulling ion-spinner"),s=this.el.querySelector("ion-refresher-content .refresher-refreshing ion-spinner");m(this)==="ios"?this.setupiOSNativeRefresher(r,s):this.setupMDNativeRefresher(e,r,s)})}componentDidUpdate(){this.checkNativeRefresher()}connectedCallback(){return f(this,null,function*(){if(this.el.getAttribute("slot")!=="fixed"){console.error('Make sure you use: <ion-refresher slot="fixed">');return}let e=this.el.closest(H);if(!e){q(this.el);return}E(e,()=>f(this,null,function*(){let r=e.querySelector(w);this.scrollEl=yield O(r??e),this.backgroundContentEl=yield e.getBackgroundElement(),this.contentFullscreen=e.fullscreen,(yield F(this.el,m(this)))?this.setupNativeRefresher(e):(this.gesture=(yield import("./chunk-F5F7W64E.js")).createGesture({el:e,gestureName:"refresher",gesturePriority:31,direction:"y",threshold:20,passive:!1,canStart:()=>this.canStart(),onStart:()=>this.onStart(),onMove:s=>this.onMove(s),onEnd:()=>this.onEnd()}),this.disabledChanged())}))})}disconnectedCallback(){this.destroyNativeRefresher(),this.scrollEl=void 0,this.gesture&&(this.gesture.destroy(),this.gesture=void 0)}complete(){return f(this,null,function*(){this.nativeRefresher?(this.needsCompletion=!0,this.pointerDown||b(()=>b(()=>this.resetNativeRefresher(this.elementToTransform,32)))):this.close(32,"120ms")})}cancel(){return f(this,null,function*(){this.nativeRefresher?this.pointerDown||b(()=>b(()=>this.resetNativeRefresher(this.elementToTransform,16))):this.close(16,"")})}getProgress(){return Promise.resolve(this.progress)}canStart(){return!(!this.scrollEl||this.state!==1||this.scrollEl.scrollTop>0)}onStart(){this.progress=0,this.state=1,this.memoizeOverflowStyle(),this.contentFullscreen&&this.backgroundContentEl&&this.backgroundContentEl.style.setProperty("--offset-top","0px")}onMove(e){if(!this.scrollEl)return;let r=e.event;if(r.touches!==void 0&&r.touches.length>1||this.state&56)return;let s=Number.isNaN(this.pullFactor)||this.pullFactor<0?1:this.pullFactor,n=e.deltaY*s;if(n<=0){if(this.progress=0,this.state=1,this.appliedStyles){this.setCss(0,"",!1,"");return}return}if(this.state===1){if(this.scrollEl.scrollTop>0){this.progress=0;return}this.state=2}if(r.cancelable&&r.preventDefault(),this.setCss(n,"0ms",!0,""),n===0){this.progress=0;return}let l=this.pullMin;if(this.progress=n/l,this.didStart||(this.didStart=!0,this.ionStart.emit()),this.ionPull.emit(),n<l){this.state=2;return}if(n>this.pullMax){this.beginRefresh();return}this.state=4}onEnd(){this.state===4?this.beginRefresh():this.state===2?this.cancel():this.state===1&&this.restoreOverflowStyle()}beginRefresh(){this.state=8,this.setCss(this.pullMin,this.snapbackDuration,!0,""),this.ionRefresh.emit({complete:this.complete.bind(this)})}close(e,r){setTimeout(()=>{var s;this.state=1,this.progress=0,this.didStart=!1,this.setCss(0,"0ms",!1,"",!0),this.contentFullscreen&&this.backgroundContentEl&&((s=this.backgroundContentEl)===null||s===void 0||s.style.removeProperty("--offset-top"))},600),this.state=e,this.setCss(0,this.closeDuration,!0,r)}setCss(e,r,s,n,l=!1){this.nativeRefresher||(this.appliedStyles=e>0,c(()=>{if(this.scrollEl&&this.backgroundContentEl){let o=this.scrollEl.style,i=this.backgroundContentEl.style;o.transform=i.transform=e>0?`translateY(${e}px) translateZ(0px)`:"",o.transitionDuration=i.transitionDuration=r,o.transitionDelay=i.transitionDelay=n,o.overflow=s?"hidden":""}l&&this.restoreOverflowStyle()}))}memoizeOverflowStyle(){if(this.scrollEl){let{overflow:e,overflowX:r,overflowY:s}=this.scrollEl.style;this.overflowStyles={overflow:e??"",overflowX:r??"",overflowY:s??""}}}restoreOverflowStyle(){if(this.overflowStyles!==void 0&&this.scrollEl!==void 0){let{overflow:e,overflowX:r,overflowY:s}=this.overflowStyles;this.scrollEl.style.overflow=e,this.scrollEl.style.overflowX=r,this.scrollEl.style.overflowY=s,this.overflowStyles=void 0}}render(){let e=m(this);return a(P,{key:"9d4ac22988aec2c6af0b0c90934c52f62f0e4ce3",slot:"fixed",class:{[e]:!0,[`refresher-${e}`]:!0,"refresher-native":this.nativeRefresher,"refresher-active":this.state!==1,"refresher-pulling":this.state===2,"refresher-ready":this.state===4,"refresher-refreshing":this.state===8,"refresher-cancelling":this.state===16,"refresher-completing":this.state===32}})}get el(){return A(this)}static get watchers(){return{disabled:["disabledChanged"]}}};return t.style={ios:ee,md:te},t})(),ye=class{constructor(t){C(this,t),this.customHTMLEnabled=y.get("innerHTMLTemplatesEnabled",Y),this.pullingIcon=void 0,this.pullingText=void 0,this.refreshingSpinner=void 0,this.refreshingText=void 0}componentWillLoad(){if(this.pullingIcon===void 0){let t=$(),e=m(this),r=t?"lines":D;this.pullingIcon=y.get("refreshingIcon",e==="ios"&&t?y.get("spinner",r):"circular")}if(this.refreshingSpinner===void 0){let t=m(this);this.refreshingSpinner=y.get("refreshingSpinner",y.get("spinner",t==="ios"?"lines":"circular"))}}renderPullingText(){let{customHTMLEnabled:t,pullingText:e}=this;return t?a("div",{class:"refresher-pulling-text",innerHTML:T(e)}):a("div",{class:"refresher-pulling-text"},e)}renderRefreshingText(){let{customHTMLEnabled:t,refreshingText:e}=this;return t?a("div",{class:"refresher-refreshing-text",innerHTML:T(e)}):a("div",{class:"refresher-refreshing-text"},e)}render(){let t=this.pullingIcon,e=t!=null&&z[t]!==void 0,r=m(this);return a(P,{key:"19633bbcf02e3dba885d6bdcdaf2269bf4c8e2f5",class:r},a("div",{key:"28922e434a55a6cac0476fe2bee56941ce0d1c02",class:"refresher-pulling"},this.pullingIcon&&e&&a("div",{key:"fe575bf996021884677e9b23a3215d63caf894f5",class:"refresher-pulling-icon"},a("div",{key:"59fe12297fd95bc33b8df8cd35316e2a1c084d91",class:"spinner-arrow-container"},a("ion-spinner",{key:"af9cc013ae04945c140b2865610ca73edb52ab48",name:this.pullingIcon,paused:!0}),r==="md"&&this.pullingIcon==="circular"&&a("div",{key:"34df66ad1b0f706a0532957251aa2c20bf4587d8",class:"arrow-container"},a("ion-icon",{key:"ffdb5c123e606b823491c6c51cc2b497f62581bb",icon:M,"aria-hidden":"true"})))),this.pullingIcon&&!e&&a("div",{key:"ac3a2032bb969264d20fa057e9123441005d7a9d",class:"refresher-pulling-icon"},a("ion-icon",{key:"d969d10915548e72aae289b52154366f3dd39b31",icon:this.pullingIcon,lazy:!1,"aria-hidden":"true"})),this.pullingText!==void 0&&this.renderPullingText()),a("div",{key:"a8f854f81a94812d7bef8ce088331d94f49ff53d",class:"refresher-refreshing"},this.refreshingSpinner&&a("div",{key:"a1f646945370e40c844d62bc0c647443ae9dfbe7",class:"refresher-refreshing-icon"},a("ion-spinner",{key:"ab3ff4047769b6436e222b46d193c1e8b23e2fce",name:this.refreshingSpinner})),this.refreshingText!==void 0&&this.renderRefreshingText()))}get el(){return A(this)}};export{ue as ion_refresher,ye as ion_refresher_content};
