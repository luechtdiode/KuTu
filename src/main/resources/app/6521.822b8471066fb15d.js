"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[6521],{333:(P,a,r)=>{r.d(a,{c:()=>c,g:()=>p,h:()=>u,o:()=>_});var i=r(467);const u=(e,t)=>null!==t.closest(e),c=(e,t)=>"string"==typeof e&&e.length>0?Object.assign({"ion-color":!0,[`ion-color-${e}`]:!0},t):t,p=e=>{const t={};return(e=>void 0!==e?(Array.isArray(e)?e:e.split(" ")).filter(s=>null!=s).map(s=>s.trim()).filter(s=>""!==s):[])(e).forEach(s=>t[s]=!0),t},h=/^[a-z][a-z0-9+\-.]*:/,_=function(){var e=(0,i.A)(function*(t,s,l,o){if(null!=t&&"#"!==t[0]&&!h.test(t)){const n=document.querySelector("ion-router");if(n)return s?.preventDefault(),n.push(t,l,o)}return!1});return function(s,l,o,n){return e.apply(this,arguments)}}()},6521:(P,a,r)=>{r.r(a),r.d(a,{ion_input_password_toggle:()=>s});var i=r(6317),u=r(9144),c=r(333),d=r(3992),p=r(4346);const s=(()=>{let l=class{constructor(o){(0,i.r)(this,o),this.togglePasswordVisibility=()=>{const{inputElRef:n}=this;n&&(n.type="text"===n.type?"password":"text")},this.color=void 0,this.showIcon=void 0,this.hideIcon=void 0,this.type="password"}onTypeChange(o){"text"===o||"password"===o||(0,u.p)(`ion-input-password-toggle only supports inputs of type "text" or "password". Input of type "${o}" is not compatible.`,this.el)}connectedCallback(){const{el:o}=this,n=this.inputElRef=o.closest("ion-input");n?this.type=n.type:(0,u.p)("No ancestor ion-input found for ion-input-password-toggle. This component must be slotted inside of an ion-input.",o)}disconnectedCallback(){this.inputElRef=null}render(){var o,n;const{color:f,type:E}=this,g=(0,p.b)(this),b=null!==(o=this.showIcon)&&void 0!==o?o:d.x,w=null!==(n=this.hideIcon)&&void 0!==n?n:d.y,y="text"===E;return(0,i.h)(i.e,{key:"d9811e25bfeb2aa197352bb9be852e9e420739d5",class:(0,c.c)(f,{[g]:!0})},(0,i.h)("ion-button",{key:"1eaea1442b248fb2b8d61538b27274e647a07804",mode:g,color:f,fill:"clear",shape:"round","aria-checked":y?"true":"false","aria-label":"show password",role:"switch",type:"button",onPointerDown:I=>{I.preventDefault()},onClick:this.togglePasswordVisibility},(0,i.h)("ion-icon",{key:"9c88de8f4631d9bde222ce2edf6950d639e04773",slot:"icon-only","aria-hidden":"true",icon:y?w:b})))}get el(){return(0,i.f)(this)}static get watchers(){return{type:["onTypeChange"]}}};return l.style={ios:"",md:""},l})()}}]);