(self.webpackChunkresultcatcher=self.webpackChunkresultcatcher||[]).push([[1349],{1349:(e,t,i)=>{"use strict";i.r(t),i.d(t,{ion_picker_column_internal:()=>a});var s=i(1035),n=i(5466),r=i(7278),o=i(1240),l=i(7853);let a=class{constructor(e){(0,s.r)(this,e),this.ionChange=(0,s.e)(this,"ionChange",7),this.hapticsStarted=!1,this.isColumnVisible=!1,this.isActive=!1,this.items=[],this.color="primary",this.numericInput=!1,this.centerPickerItemInView=(e,t=!0)=>{this.el.scroll({top:e.offsetTop-3*e.clientHeight+e.clientHeight/2,left:0,behavior:t?"smooth":void 0})},this.inputModeChange=e=>{if(!this.numericInput)return;const{useInputMode:t,inputModeColumn:i}=e.detail;this.isActive=!(!t||void 0!==i&&i!==this.el)},this.initializeScrollListener=()=>{const{el:e}=this;let t,i=this.activeItem;const s=()=>{(0,r.r)(()=>{t&&(clearTimeout(t),t=void 0),this.hapticsStarted||((0,o.a)(),this.hapticsStarted=!0);const s=e.getBoundingClientRect(),n=e.shadowRoot.elementFromPoint(s.x+s.width/2,s.y+s.height/2);null!==i&&i.classList.remove(c),n!==i&&(0,o.b)(),i=n,n.classList.add(c),t=setTimeout(()=>{const e=n.getAttribute("data-index");if(null===e)return;const t=parseInt(e,10),i=this.items[t];i.value!==this.value&&(this.value=i.value,(0,o.h)(),this.hapticsStarted=!1)},250)})};(0,r.r)(()=>{e.addEventListener("scroll",s),this.destroyScrollListener=()=>{e.removeEventListener("scroll",s)}})}}valueChange(){if(this.isColumnVisible){const{items:e,value:t}=this;this.scrollActiveItemIntoView();const i=e.find(e=>e.value===t);i&&this.ionChange.emit(i)}}componentWillLoad(){new IntersectionObserver(e=>{var t;if(e[0].isIntersecting){const e=(0,r.g)(this.el).querySelector(`.${c}`);null==e||e.classList.remove(c),this.scrollActiveItemIntoView(),null===(t=this.activeItem)||void 0===t||t.classList.add(c),this.initializeScrollListener(),this.isColumnVisible=!0}else this.destroyScrollListener&&(this.destroyScrollListener(),this.destroyScrollListener=void 0),this.isColumnVisible=!1},{threshold:.01}).observe(this.el);const e=this.el.closest("ion-picker-internal");null!==e&&e.addEventListener("ionInputModeChange",e=>this.inputModeChange(e))}scrollActiveItemIntoView(){const e=this.activeItem;e&&this.centerPickerItemInView(e,!1)}get activeItem(){return(0,r.g)(this.el).querySelector(`.picker-item[data-value="${this.value}"]`)}render(){const{items:e,color:t,isActive:i,numericInput:r}=this,o=(0,n.b)(this);return(0,s.h)(s.H,{tabindex:0,class:(0,l.c)(t,{[o]:!0,"picker-column-active":i,"picker-column-numeric-input":r})},(0,s.h)("div",{class:"picker-item picker-item-empty"},"\xa0"),(0,s.h)("div",{class:"picker-item picker-item-empty"},"\xa0"),(0,s.h)("div",{class:"picker-item picker-item-empty"},"\xa0"),e.map((e,t)=>(0,s.h)("div",{class:"picker-item","data-value":e.value,"data-index":t,onClick:e=>{this.centerPickerItemInView(e.target)}},e.text)),(0,s.h)("div",{class:"picker-item picker-item-empty"},"\xa0"),(0,s.h)("div",{class:"picker-item picker-item-empty"},"\xa0"),(0,s.h)("div",{class:"picker-item picker-item-empty"},"\xa0"))}get el(){return(0,s.i)(this)}static get watchers(){return{value:["valueChange"]}}};const c="picker-item-active";a.style={ios:":host{padding-left:16px;padding-right:16px;padding-top:0px;padding-bottom:0px;height:200px;outline:none;font-size:22px;-webkit-scroll-snap-type:y mandatory;-ms-scroll-snap-type:y mandatory;scroll-snap-type:y mandatory;overflow-x:hidden;overflow-y:scroll;scrollbar-width:none;text-align:center}@supports ((-webkit-margin-start: 0) or (margin-inline-start: 0)) or (-webkit-margin-start: 0){:host{padding-left:unset;padding-right:unset;-webkit-padding-start:16px;padding-inline-start:16px;-webkit-padding-end:16px;padding-inline-end:16px}}:host::-webkit-scrollbar{display:none}:host .picker-item{height:34px;line-height:34px;text-overflow:ellipsis;white-space:nowrap;overflow:hidden;scroll-snap-align:center}:host .picker-item-empty{scroll-snap-align:none}:host(.picker-column-active) .picker-item.picker-item-active{color:var(--ion-color-base)}@media (any-hover: hover){:host(:focus){outline:none;background:rgba(var(--ion-color-base-rgb), 0.2)}}",md:":host{padding-left:16px;padding-right:16px;padding-top:0px;padding-bottom:0px;height:200px;outline:none;font-size:22px;-webkit-scroll-snap-type:y mandatory;-ms-scroll-snap-type:y mandatory;scroll-snap-type:y mandatory;overflow-x:hidden;overflow-y:scroll;scrollbar-width:none;text-align:center}@supports ((-webkit-margin-start: 0) or (margin-inline-start: 0)) or (-webkit-margin-start: 0){:host{padding-left:unset;padding-right:unset;-webkit-padding-start:16px;padding-inline-start:16px;-webkit-padding-end:16px;padding-inline-end:16px}}:host::-webkit-scrollbar{display:none}:host .picker-item{height:34px;line-height:34px;text-overflow:ellipsis;white-space:nowrap;overflow:hidden;scroll-snap-align:center}:host .picker-item-empty{scroll-snap-align:none}:host(.picker-column-active) .picker-item.picker-item-active{color:var(--ion-color-base)}@media (any-hover: hover){:host(:focus){outline:none;background:rgba(var(--ion-color-base-rgb), 0.2)}}:host .picker-item-active{color:var(--ion-color-base)}"}}}]);