(self.webpackChunkresultcatcher=self.webpackChunkresultcatcher||[]).push([[1296],{1296:(e,t,i)=>{"use strict";i.r(t),i.d(t,{KEYBOARD_DID_CLOSE:()=>o,KEYBOARD_DID_OPEN:()=>s,copyVisualViewport:()=>D,keyboardDidClose:()=>w,keyboardDidOpen:()=>g,keyboardDidResize:()=>l,resetKeyboardAssist:()=>h,setKeyboardClose:()=>b,setKeyboardOpen:()=>c,startKeyboardAssist:()=>n,trackViewportChanges:()=>y});const s="ionKeyboardDidShow",o="ionKeyboardDidHide";let a={},d={},r=!1;const h=()=>{a={},d={},r=!1},n=e=>{p(e),e.visualViewport&&(d=D(e.visualViewport),e.visualViewport.onresize=()=>{y(e),g()||l(e)?c(e):w(e)&&b(e)})},p=e=>{e.addEventListener("keyboardDidShow",t=>c(e,t)),e.addEventListener("keyboardDidHide",()=>b(e))},c=(e,t)=>{u(e,t),r=!0},b=e=>{f(e),r=!1},g=()=>!r&&a.width===d.width&&(a.height-d.height)*d.scale>150,l=e=>r&&!w(e),w=e=>r&&d.height===e.innerHeight,u=(e,t)=>{const i=new CustomEvent(s,{detail:{keyboardHeight:t?t.keyboardHeight:e.innerHeight-d.height}});e.dispatchEvent(i)},f=e=>{const t=new CustomEvent(o);e.dispatchEvent(t)},y=e=>{a=Object.assign({},d),d=D(e.visualViewport)},D=e=>({width:Math.round(e.width),height:Math.round(e.height),offsetTop:e.offsetTop,offsetLeft:e.offsetLeft,pageTop:e.pageTop,pageLeft:e.pageLeft,scale:e.scale})}}]);