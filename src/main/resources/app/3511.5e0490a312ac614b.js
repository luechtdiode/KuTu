"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[3511],{333:(y,l,a)=>{a.d(l,{c:()=>h,g:()=>k,h:()=>e,o:()=>m});var x=a(467);const e=(c,r)=>null!==r.closest(c),h=(c,r)=>"string"==typeof c&&c.length>0?Object.assign({"ion-color":!0,[`ion-color-${c}`]:!0},r):r,k=c=>{const r={};return(c=>void 0!==c?(Array.isArray(c)?c:c.split(" ")).filter(i=>null!=i).map(i=>i.trim()).filter(i=>""!==i):[])(c).forEach(i=>r[i]=!0),r},u=/^[a-z][a-z0-9+\-.]*:/,m=function(){var c=(0,x.A)(function*(r,i,p,o){if(null!=r&&"#"!==r[0]&&!u.test(r)){const t=document.querySelector("ion-router");if(t)return i?.preventDefault(),t.push(r,p,o)}return!1});return function(i,p,o,t){return c.apply(this,arguments)}}()},3511:(y,l,a)=>{a.r(l),a.d(l,{ion_checkbox:()=>i});var x=a(467),e=a(6317),h=a(1559),b=a(333),k=a(4346);const i=class{constructor(o){(0,e.r)(this,o),this.ionChange=(0,e.c)(this,"ionChange",7),this.ionFocus=(0,e.c)(this,"ionFocus",7),this.ionBlur=(0,e.c)(this,"ionBlur",7),this.inputId="ion-cb-"+p++,this.helperTextId=`${this.inputId}-helper-text`,this.errorTextId=`${this.inputId}-error-text`,this.inheritedAttributes={},this.setChecked=t=>{const n=this.checked=t;this.ionChange.emit({checked:n,value:this.value})},this.toggleChecked=t=>{t.preventDefault(),this.setFocus(),this.setChecked(!this.checked),this.indeterminate=!1},this.onFocus=()=>{this.ionFocus.emit()},this.onBlur=()=>{this.ionBlur.emit()},this.onClick=t=>{this.disabled||this.toggleChecked(t)},this.color=void 0,this.name=this.inputId,this.checked=!1,this.indeterminate=!1,this.disabled=!1,this.errorText=void 0,this.helperText=void 0,this.value="on",this.labelPlacement="start",this.justify=void 0,this.alignment=void 0,this.required=!1}componentWillLoad(){this.inheritedAttributes=Object.assign({},(0,h.i)(this.el))}setFocus(){var o=this;return(0,x.A)(function*(){o.focusEl&&o.focusEl.focus()})()}getHintTextID(){const{el:o,helperText:t,errorText:n,helperTextId:s,errorTextId:d}=this;return o.classList.contains("ion-touched")&&o.classList.contains("ion-invalid")&&n?d:t?s:void 0}renderHintText(){const{helperText:o,errorText:t,helperTextId:n,errorTextId:s}=this;if(o||t)return(0,e.h)("div",{class:"checkbox-bottom"},(0,e.h)("div",{id:n,class:"helper-text",part:"supporting-text helper-text"},o),(0,e.h)("div",{id:s,class:"error-text",part:"supporting-text error-text"},t))}render(){const{color:o,checked:t,disabled:n,el:s,getSVGPath:d,indeterminate:f,inheritedAttributes:_,inputId:j,justify:g,labelPlacement:C,name:T,value:I,alignment:w,required:z}=this,v=(0,k.b)(this),E=d(v,f);return(0,h.d)(!0,s,T,t?I:"",n),(0,e.h)(e.e,{key:"7ac31df89b07c781ddcd30a6a8c109494d0c500a","aria-checked":f?"mixed":`${t}`,"aria-describedby":this.getHintTextID(),"aria-invalid":this.getHintTextID()===this.errorTextId,class:(0,b.c)(o,{[v]:!0,"in-item":(0,b.h)("ion-item",s),"checkbox-checked":t,"checkbox-disabled":n,"checkbox-indeterminate":f,interactive:!0,[`checkbox-justify-${g}`]:void 0!==g,[`checkbox-alignment-${w}`]:void 0!==w,[`checkbox-label-placement-${C}`]:!0}),onClick:this.onClick},(0,e.h)("label",{key:"674e923fe1ec83a33c31d67b0d414d61ba8f9e4b",class:"checkbox-wrapper"},(0,e.h)("input",Object.assign({key:"c4866e392fbdf3b76edcd1507cb67f40a213a4e7",type:"checkbox",checked:!!t||void 0,disabled:n,id:j,onChange:this.toggleChecked,onFocus:()=>this.onFocus(),onBlur:()=>this.onBlur(),ref:M=>this.focusEl=M,required:z},_)),(0,e.h)("div",{key:"79cb96e5963b9331a760438ec8cc9e456215de91",class:{"label-text-wrapper":!0,"label-text-wrapper-hidden":""===s.textContent},part:"label"},(0,e.h)("slot",{key:"896cb26292c9a4a6c105afb39611472b93bf5e90"}),this.renderHintText()),(0,e.h)("div",{key:"52cd22e79fd5db30b45d7b092aa5af3944392336",class:"native-wrapper"},(0,e.h)("svg",{key:"18d862ab7cc32055eaf200eea560ff1b2b6cbde0",class:"checkbox-icon",viewBox:"0 0 24 24",part:"container"},E))))}getSVGPath(o,t){let n=(0,e.h)("path",t?{d:"M6 12L18 12",part:"mark"}:{d:"M5.9,12.5l3.8,3.8l8.8-8.8",part:"mark"});return"md"===o&&(n=(0,e.h)("path",t?{d:"M2 12H22",part:"mark"}:{d:"M1.73,12.91 8.1,19.28 22.79,4.59",part:"mark"})),n}get el(){return(0,e.f)(this)}};let p=0;i.style={ios:":host{--checkbox-background-checked:var(--ion-color-primary, #0054e9);--border-color-checked:var(--ion-color-primary, #0054e9);--checkmark-color:var(--ion-color-primary-contrast, #fff);--transition:none;display:inline-block;position:relative;cursor:pointer;-webkit-user-select:none;-moz-user-select:none;-ms-user-select:none;user-select:none;z-index:2}:host(.in-item){-ms-flex:1 1 0px;flex:1 1 0;width:100%;height:100%}:host([slot=start]),:host([slot=end]){-ms-flex:initial;flex:initial;width:auto}:host(.ion-color){--checkbox-background-checked:var(--ion-color-base);--border-color-checked:var(--ion-color-base);--checkmark-color:var(--ion-color-contrast)}.checkbox-wrapper{display:-ms-flexbox;display:flex;-ms-flex-positive:1;flex-grow:1;-ms-flex-align:center;align-items:center;-ms-flex-pack:justify;justify-content:space-between;height:inherit;cursor:inherit}.label-text-wrapper{text-overflow:ellipsis;white-space:nowrap;overflow:hidden}:host(.in-item) .label-text-wrapper,:host(.in-item:not(.checkbox-label-placement-stacked):not([slot])) .native-wrapper{margin-top:10px;margin-bottom:10px}:host(.in-item.checkbox-label-placement-stacked) .label-text-wrapper{margin-top:10px;margin-bottom:16px}:host(.in-item.checkbox-label-placement-stacked) .native-wrapper{margin-bottom:10px}.label-text-wrapper-hidden{display:none}input{position:absolute;top:0;left:0;right:0;bottom:0;width:100%;height:100%;margin:0;padding:0;border:0;outline:0;clip:rect(0 0 0 0);opacity:0;overflow:hidden;-webkit-appearance:none;-moz-appearance:none}.native-wrapper{display:-ms-flexbox;display:flex;-ms-flex-align:center;align-items:center}.checkbox-icon{border-radius:var(--border-radius);position:relative;width:var(--size);height:var(--size);-webkit-transition:var(--transition);transition:var(--transition);border-width:var(--border-width);border-style:var(--border-style);border-color:var(--border-color);background:var(--checkbox-background);-webkit-box-sizing:border-box;box-sizing:border-box}.checkbox-icon path{fill:none;stroke:var(--checkmark-color);stroke-width:var(--checkmark-width);opacity:0}.checkbox-bottom{padding-top:4px;display:-ms-flexbox;display:flex;-ms-flex-pack:justify;justify-content:space-between;font-size:0.75rem;white-space:normal}:host(.checkbox-label-placement-stacked) .checkbox-bottom{font-size:1rem}.checkbox-bottom .error-text{display:none;color:var(--ion-color-danger, #c5000f)}.checkbox-bottom .helper-text{display:block;color:var(--ion-color-step-700, var(--ion-text-color-step-300, #4d4d4d))}:host(.ion-touched.ion-invalid) .checkbox-bottom .error-text{display:block}:host(.ion-touched.ion-invalid) .checkbox-bottom .helper-text{display:none}:host(.checkbox-label-placement-start) .checkbox-wrapper{-ms-flex-direction:row;flex-direction:row}:host(.checkbox-label-placement-start) .label-text-wrapper{-webkit-margin-start:0;margin-inline-start:0;-webkit-margin-end:16px;margin-inline-end:16px}:host(.checkbox-label-placement-end) .checkbox-wrapper{-ms-flex-direction:row-reverse;flex-direction:row-reverse;-ms-flex-pack:start;justify-content:start}:host(.checkbox-label-placement-end) .label-text-wrapper{-webkit-margin-start:16px;margin-inline-start:16px;-webkit-margin-end:0;margin-inline-end:0}:host(.checkbox-label-placement-fixed) .label-text-wrapper{-webkit-margin-start:0;margin-inline-start:0;-webkit-margin-end:16px;margin-inline-end:16px}:host(.checkbox-label-placement-fixed) .label-text-wrapper{-ms-flex:0 0 100px;flex:0 0 100px;width:100px;min-width:100px;max-width:200px}:host(.checkbox-label-placement-stacked) .checkbox-wrapper{-ms-flex-direction:column;flex-direction:column;text-align:center}:host(.checkbox-label-placement-stacked) .label-text-wrapper{-webkit-transform:scale(0.75);transform:scale(0.75);margin-left:0;margin-right:0;margin-bottom:16px;max-width:calc(100% / 0.75)}:host(.checkbox-label-placement-stacked.checkbox-alignment-start) .label-text-wrapper{-webkit-transform-origin:left top;transform-origin:left top}:host-context([dir=rtl]):host(.checkbox-label-placement-stacked.checkbox-alignment-start) .label-text-wrapper,:host-context([dir=rtl]).checkbox-label-placement-stacked.checkbox-alignment-start .label-text-wrapper{-webkit-transform-origin:right top;transform-origin:right top}@supports selector(:dir(rtl)){:host(.checkbox-label-placement-stacked.checkbox-alignment-start:dir(rtl)) .label-text-wrapper{-webkit-transform-origin:right top;transform-origin:right top}}:host(.checkbox-label-placement-stacked.checkbox-alignment-center) .label-text-wrapper{-webkit-transform-origin:center top;transform-origin:center top}:host-context([dir=rtl]):host(.checkbox-label-placement-stacked.checkbox-alignment-center) .label-text-wrapper,:host-context([dir=rtl]).checkbox-label-placement-stacked.checkbox-alignment-center .label-text-wrapper{-webkit-transform-origin:calc(100% - center) top;transform-origin:calc(100% - center) top}@supports selector(:dir(rtl)){:host(.checkbox-label-placement-stacked.checkbox-alignment-center:dir(rtl)) .label-text-wrapper{-webkit-transform-origin:calc(100% - center) top;transform-origin:calc(100% - center) top}}:host(.checkbox-justify-space-between) .checkbox-wrapper{-ms-flex-pack:justify;justify-content:space-between}:host(.checkbox-justify-start) .checkbox-wrapper{-ms-flex-pack:start;justify-content:start}:host(.checkbox-justify-end) .checkbox-wrapper{-ms-flex-pack:end;justify-content:end}:host(.checkbox-alignment-start) .checkbox-wrapper{-ms-flex-align:start;align-items:start}:host(.checkbox-alignment-center) .checkbox-wrapper{-ms-flex-align:center;align-items:center}:host(.checkbox-justify-space-between),:host(.checkbox-justify-start),:host(.checkbox-justify-end),:host(.checkbox-alignment-start),:host(.checkbox-alignment-center){display:block}:host(.checkbox-checked) .checkbox-icon,:host(.checkbox-indeterminate) .checkbox-icon{border-color:var(--border-color-checked);background:var(--checkbox-background-checked)}:host(.checkbox-checked) .checkbox-icon path,:host(.checkbox-indeterminate) .checkbox-icon path{opacity:1}:host(.checkbox-disabled){pointer-events:none}:host{--border-radius:50%;--border-width:0.125rem;--border-style:solid;--border-color:rgba(var(--ion-text-color-rgb, 0, 0, 0), 0.23);--checkbox-background:var(--ion-item-background, var(--ion-background-color, #fff));--size:min(1.375rem, 55.836px);--checkmark-width:1.5px}:host(.checkbox-disabled){opacity:0.3}",md:":host{--checkbox-background-checked:var(--ion-color-primary, #0054e9);--border-color-checked:var(--ion-color-primary, #0054e9);--checkmark-color:var(--ion-color-primary-contrast, #fff);--transition:none;display:inline-block;position:relative;cursor:pointer;-webkit-user-select:none;-moz-user-select:none;-ms-user-select:none;user-select:none;z-index:2}:host(.in-item){-ms-flex:1 1 0px;flex:1 1 0;width:100%;height:100%}:host([slot=start]),:host([slot=end]){-ms-flex:initial;flex:initial;width:auto}:host(.ion-color){--checkbox-background-checked:var(--ion-color-base);--border-color-checked:var(--ion-color-base);--checkmark-color:var(--ion-color-contrast)}.checkbox-wrapper{display:-ms-flexbox;display:flex;-ms-flex-positive:1;flex-grow:1;-ms-flex-align:center;align-items:center;-ms-flex-pack:justify;justify-content:space-between;height:inherit;cursor:inherit}.label-text-wrapper{text-overflow:ellipsis;white-space:nowrap;overflow:hidden}:host(.in-item) .label-text-wrapper,:host(.in-item:not(.checkbox-label-placement-stacked):not([slot])) .native-wrapper{margin-top:10px;margin-bottom:10px}:host(.in-item.checkbox-label-placement-stacked) .label-text-wrapper{margin-top:10px;margin-bottom:16px}:host(.in-item.checkbox-label-placement-stacked) .native-wrapper{margin-bottom:10px}.label-text-wrapper-hidden{display:none}input{position:absolute;top:0;left:0;right:0;bottom:0;width:100%;height:100%;margin:0;padding:0;border:0;outline:0;clip:rect(0 0 0 0);opacity:0;overflow:hidden;-webkit-appearance:none;-moz-appearance:none}.native-wrapper{display:-ms-flexbox;display:flex;-ms-flex-align:center;align-items:center}.checkbox-icon{border-radius:var(--border-radius);position:relative;width:var(--size);height:var(--size);-webkit-transition:var(--transition);transition:var(--transition);border-width:var(--border-width);border-style:var(--border-style);border-color:var(--border-color);background:var(--checkbox-background);-webkit-box-sizing:border-box;box-sizing:border-box}.checkbox-icon path{fill:none;stroke:var(--checkmark-color);stroke-width:var(--checkmark-width);opacity:0}.checkbox-bottom{padding-top:4px;display:-ms-flexbox;display:flex;-ms-flex-pack:justify;justify-content:space-between;font-size:0.75rem;white-space:normal}:host(.checkbox-label-placement-stacked) .checkbox-bottom{font-size:1rem}.checkbox-bottom .error-text{display:none;color:var(--ion-color-danger, #c5000f)}.checkbox-bottom .helper-text{display:block;color:var(--ion-color-step-700, var(--ion-text-color-step-300, #4d4d4d))}:host(.ion-touched.ion-invalid) .checkbox-bottom .error-text{display:block}:host(.ion-touched.ion-invalid) .checkbox-bottom .helper-text{display:none}:host(.checkbox-label-placement-start) .checkbox-wrapper{-ms-flex-direction:row;flex-direction:row}:host(.checkbox-label-placement-start) .label-text-wrapper{-webkit-margin-start:0;margin-inline-start:0;-webkit-margin-end:16px;margin-inline-end:16px}:host(.checkbox-label-placement-end) .checkbox-wrapper{-ms-flex-direction:row-reverse;flex-direction:row-reverse;-ms-flex-pack:start;justify-content:start}:host(.checkbox-label-placement-end) .label-text-wrapper{-webkit-margin-start:16px;margin-inline-start:16px;-webkit-margin-end:0;margin-inline-end:0}:host(.checkbox-label-placement-fixed) .label-text-wrapper{-webkit-margin-start:0;margin-inline-start:0;-webkit-margin-end:16px;margin-inline-end:16px}:host(.checkbox-label-placement-fixed) .label-text-wrapper{-ms-flex:0 0 100px;flex:0 0 100px;width:100px;min-width:100px;max-width:200px}:host(.checkbox-label-placement-stacked) .checkbox-wrapper{-ms-flex-direction:column;flex-direction:column;text-align:center}:host(.checkbox-label-placement-stacked) .label-text-wrapper{-webkit-transform:scale(0.75);transform:scale(0.75);margin-left:0;margin-right:0;margin-bottom:16px;max-width:calc(100% / 0.75)}:host(.checkbox-label-placement-stacked.checkbox-alignment-start) .label-text-wrapper{-webkit-transform-origin:left top;transform-origin:left top}:host-context([dir=rtl]):host(.checkbox-label-placement-stacked.checkbox-alignment-start) .label-text-wrapper,:host-context([dir=rtl]).checkbox-label-placement-stacked.checkbox-alignment-start .label-text-wrapper{-webkit-transform-origin:right top;transform-origin:right top}@supports selector(:dir(rtl)){:host(.checkbox-label-placement-stacked.checkbox-alignment-start:dir(rtl)) .label-text-wrapper{-webkit-transform-origin:right top;transform-origin:right top}}:host(.checkbox-label-placement-stacked.checkbox-alignment-center) .label-text-wrapper{-webkit-transform-origin:center top;transform-origin:center top}:host-context([dir=rtl]):host(.checkbox-label-placement-stacked.checkbox-alignment-center) .label-text-wrapper,:host-context([dir=rtl]).checkbox-label-placement-stacked.checkbox-alignment-center .label-text-wrapper{-webkit-transform-origin:calc(100% - center) top;transform-origin:calc(100% - center) top}@supports selector(:dir(rtl)){:host(.checkbox-label-placement-stacked.checkbox-alignment-center:dir(rtl)) .label-text-wrapper{-webkit-transform-origin:calc(100% - center) top;transform-origin:calc(100% - center) top}}:host(.checkbox-justify-space-between) .checkbox-wrapper{-ms-flex-pack:justify;justify-content:space-between}:host(.checkbox-justify-start) .checkbox-wrapper{-ms-flex-pack:start;justify-content:start}:host(.checkbox-justify-end) .checkbox-wrapper{-ms-flex-pack:end;justify-content:end}:host(.checkbox-alignment-start) .checkbox-wrapper{-ms-flex-align:start;align-items:start}:host(.checkbox-alignment-center) .checkbox-wrapper{-ms-flex-align:center;align-items:center}:host(.checkbox-justify-space-between),:host(.checkbox-justify-start),:host(.checkbox-justify-end),:host(.checkbox-alignment-start),:host(.checkbox-alignment-center){display:block}:host(.checkbox-checked) .checkbox-icon,:host(.checkbox-indeterminate) .checkbox-icon{border-color:var(--border-color-checked);background:var(--checkbox-background-checked)}:host(.checkbox-checked) .checkbox-icon path,:host(.checkbox-indeterminate) .checkbox-icon path{opacity:1}:host(.checkbox-disabled){pointer-events:none}:host{--border-radius:calc(var(--size) * .125);--border-width:2px;--border-style:solid;--border-color:rgb(var(--ion-text-color-rgb, 0, 0, 0), 0.6);--checkmark-width:3;--checkbox-background:var(--ion-item-background, var(--ion-background-color, #fff));--transition:background 180ms cubic-bezier(0.4, 0, 0.2, 1);--size:18px}.checkbox-icon path{stroke-dasharray:30;stroke-dashoffset:30}:host(.checkbox-checked) .checkbox-icon path,:host(.checkbox-indeterminate) .checkbox-icon path{stroke-dashoffset:0;-webkit-transition:stroke-dashoffset 90ms linear 90ms;transition:stroke-dashoffset 90ms linear 90ms}:host(.checkbox-disabled) .label-text-wrapper{opacity:0.38}:host(.checkbox-disabled) .native-wrapper{opacity:0.63}"}}}]);