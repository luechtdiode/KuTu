(self.webpackChunkresultcatcher=self.webpackChunkresultcatcher||[]).push([[5024],{5024:(e,t,n)=>{"use strict";n.r(t),n.d(t,{ClubregEditorPageModule:()=>ne});var i=n(8583),o=n(665),s=n(881),l=n(2859),a=n(4762),r=n(5257),c=n(7225),u=n(8186),h=n(7709),d=n(4402),p=n(7574),m=n(8939);const g=["searchbarElem"],f=["inputElem"],b=["itemList"];function v(e,t){if(1&e&&u._UZ(0,"ion-icon",15),2&e){const e=u.oxw(2);u.Q6J("name",e.frontIcon)("slot","start")}}function w(e,t){if(1&e&&u._UZ(0,"ion-icon",16),2&e){const e=u.oxw().attrs;u.Q6J("name",e.removeButtonIcon)("slot",e.removeButtonSlot)}}function O(e,t){if(1&e&&(u.TgZ(0,"ion-chip",12),u.YNc(1,v,1,2,"ion-icon",13),u.TgZ(2,"ion-label"),u._uU(3),u.qZA(),u.YNc(4,w,1,2,"ion-icon",14),u.qZA()),2&e){const e=t.attrs,n=u.oxw();u.Tol(e.removeButtonClasses),u.Q6J("color",e.removeButtonColor)("outline",!0),u.xp6(1),u.Q6J("ngIf",n.frontIcon),u.xp6(2),u.Oqu(e.label),u.xp6(1),u.Q6J("ngIf",e.removeButtonIcon)}}function y(e,t){}const C=function(e,t,n,i,o,s){return{data:e,label:t,removeButtonClasses:n,removeButtonColor:i,removeButtonIcon:o,removeButtonSlot:s}},I=function(e){return{attrs:e}};function x(e,t){if(1&e){const e=u.EpF();u.TgZ(0,"div",18),u.NdJ("click",function(){const t=u.CHM(e).$implicit;return u.oxw(2).removeItem(t)}),u.YNc(1,y,0,0,"ng-template",10),u.qZA()}if(2&e){const e=t.$implicit,n=u.oxw(2),i=u.MAs(1);u.xp6(1),u.Q6J("ngTemplateOutlet",n.selectionTemplate||i)("ngTemplateOutletContext",u.VKq(9,I,u.HTZ(2,C,e,n.getLabel(e),n.removeButtonClasses,n.removeButtonColor,n.removeButtonIcon,n.removeButtonSlot)))}}function k(e,t){if(1&e&&(u.TgZ(0,"div"),u.YNc(1,x,2,11,"div",17),u.qZA()),2&e){const e=u.oxw();u.xp6(1),u.Q6J("ngForOf",e.selected)}}function S(e,t){if(1&e&&(u.TgZ(0,"ion-label",19),u._uU(1),u.qZA()),2&e){const e=u.oxw();u.Q6J("position",e.labelPosition),u.xp6(1),u.Oqu(e.label)}}function Z(e,t){if(1&e&&(u._UZ(0,"span",20),u.ALo(1,"boldprefix")),2&e){const e=t.attrs;u.Q6J("innerHTML",u.xi3(1,1,e.label,e.keyword),u.oJD)}}function A(e,t){if(1&e&&u._uU(0),2&e){const e=u.oxw();u.hij(" ",e.autocompleteOptions.noItems,"\n")}}function T(e,t){}function M(e,t){}const E=function(e){return{focus:e}},P=function(e,t,n,i,o,s){return{data:e,label:t,keyword:n,formValue:i,labelAttribute:o,formValueAttribute:s}};function _(e,t){if(1&e){const e=u.EpF();u.TgZ(0,"li",25),u.NdJ("mouseenter",function(){const t=u.CHM(e).index;return u.oxw(3).focusedOption=t})("click",function(t){const n=u.CHM(e).$implicit;return u.oxw(3).handleSelectTap(t,n)})("tap",function(t){const n=u.CHM(e).$implicit;return u.oxw(3).handleSelectTap(t,n)}),u.YNc(1,M,0,0,"ng-template",10),u.qZA()}if(2&e){const e=t.$implicit,n=t.index,i=u.oxw(3),o=u.MAs(9);u.Q6J("ngClass",u.VKq(4,E,i.focusedOption===n))("ngStyle",i.styles.listItem),u.xp6(1),u.Q6J("ngTemplateOutlet",i.template||o)("ngTemplateOutletContext",u.VKq(13,I,u.HTZ(6,P,e,i.getLabel(e),i.keyword,i.getFormValue(e),i.getLabel(e),i.getFormValue(e))))}}function L(e,t){if(1&e&&(u.TgZ(0,"ul",22,23),u.YNc(2,_,2,15,"li",24),u.ALo(3,"slice"),u.qZA()),2&e){const e=u.oxw().attrs,t=u.oxw();u.Q6J("ngStyle",t.listStyles()),u.xp6(2),u.Q6J("ngForOf",u.Dn7(3,2,e.data,0,e.maxResults))}}function B(e,t){}const q=function(e){return{keyword:e}};function J(e,t){if(1&e&&(u.TgZ(0,"ul",22),u.TgZ(1,"li",22),u.YNc(2,B,0,0,"ng-template",10),u.qZA(),u.qZA()),2&e){const e=u.oxw(2),t=u.MAs(11);u.Q6J("ngStyle",e.listStyles()),u.xp6(1),u.Q6J("ngStyle",e.styles.listItem),u.xp6(1),u.Q6J("ngTemplateOutlet",e.emptyTemplate||t)("ngTemplateOutletContext",u.VKq(6,I,u.VKq(4,q,e.keyword)))}}function V(e,t){if(1&e&&(u.YNc(0,L,4,6,"ul",21),u.YNc(1,J,3,8,"ul",21)),2&e){const e=t.attrs,n=u.oxw();u.Q6J("ngIf",!(n.disabled||null!==e.maxSelected&&e.selected.length>=e.maxSelected)&&e.data.length>0&&e.showSuggestions),u.xp6(1),u.Q6J("ngIf",!n.isLoading&&0===n.suggestions.length&&n.showSuggestions)}}const F=function(e,t){return{hidden:e,loading:t}},R=function(e,t,n){return{hidden:e,loading:t,disabled:n}},N=function(e,t,n,i,o,s){return{data:e,keyword:t,maxResults:n,maxSelected:i,selected:o,showSuggestions:s}};class U{constructor(){this.animated=!1,this.color=null,this.autocomplete="off",this.autocorrect="off",this.cancelButtonIcon="arrow-round-back",this.cancelButtonText="Cancel",this.clearIcon="close-circle",this.clearInput=!1,this.clearOnEdit=!1,this.debounce=250,this.mode="md",this.noItems="No items found.",this.placeholder="Search",this.searchIcon="search",this.showCancelButton=!1,this.spellcheck="off",this.type="search"}}class Q{constructor(){this.list={},this.listItem={},this.searchbar={}}}let Y=(()=>{class e{constructor(){this.autocompleteOptions=new U,this.hasFocus=!1,this.isLoading=!1,this.focusedOption=-1,this.showSuggestions=!1,this.onTouchedCallback=!1,this.onChangeCallback=!1,this.showListChanged=!1,this.alwaysShowList=!1,this.autoFocusSuggestion=!0,this.clearInvalidInput=!0,this.disabled=!1,this.exclude=[],this.frontIcon=!1,this.hideListOnSelection=!0,this.label="",this.labelPosition="fixed",this.location="auto",this.maxResults=8,this.maxSelected=null,this.multi=!1,this.name="",this.removeButtonClasses="",this.removeButtonColor="primary",this.removeButtonIcon="close-circle",this.removeButtonSlot="end",this.removeDuplicateSuggestions=!0,this.selectOnTabOut=!0,this.styles=new Q,this.useIonInput=!1,this.autoBlur=new u.vpe,this.autoFocus=new u.vpe,this.blur=new u.vpe,this.focus=new u.vpe,this.ionAutoInput=new u.vpe,this.itemsChange=new u.vpe,this.itemsCleared=new u.vpe,this.itemsHidden=new u.vpe,this.itemRemoved=new u.vpe,this.itemSelected=new u.vpe,this.itemsShown=new u.vpe,this.modelChange=new u.vpe,this.keyword="",this.suggestions=[]}set dataProvider(e){void 0!==e&&(this.provider=e,void 0!==this.selected&&(this.keyword=this.getLabel(this.selected)))}set eager(e){e&&this.getItems(null,!1)}set options(e){this.autocompleteOptions=new U;const t=Object.keys(e),n=t.length;for(let i=0;i<n;i++){const n=t[i];void 0!==e[n]&&(this.autocompleteOptions[n]=e[n])}void 0===this.selected&&(this.selected=void 0!==this.autocompleteOptions.value?this.autocompleteOptions.value:this.multi?[]:null,this.keyword=this.getLabel(this.selected)),this.autocomplete=this.autocompleteOptions.autocomplete?this.autocompleteOptions.autocomplete:"off"}get model(){let e=this.selected;return this.multi||void 0===this.selected.length||(e=0===this.selected.length?null:this.selected[0]),e}set model(e){this.selected=e}get showList(){return this.showSuggestions}set showList(e){void 0!==e&&this.showSuggestions!==e&&(this.showSuggestions=!0===e,this.showListChanged=!0)}documentClickHandler(e){this.isEventWithinElement(this.searchbarElem,e)||this.isEventWithinElement(this.inputElem,e)||this.isEventWithinElement(this.itemList,e)?this.setSuggestions(this.suggestions):this.hideItemList()}ngAfterViewChecked(){this.showListChanged&&(this.showListChanged=!1,this.showSuggestions?this.itemsShown.emit():this.itemsHidden.emit())}ngDoCheck(){this.hasFocus||this.clearInvalidInput&&(null===this.selected||this.multi)&&(""!==this.keyword&&(this.keyword=""),this.inputElem&&this.inputElem.nativeElement&&this.inputElem.nativeElement.children&&0!==this.inputElem.nativeElement.children.length&&this.inputElem.nativeElement.children[0].children&&0!==this.inputElem.nativeElement.children[0].children.length&&this.inputElem.nativeElement.children[0].children[0].value&&(this.inputElem.nativeElement.children[0].children[0].value=""),this.searchbarElem&&this.searchbarElem.nativeElement&&this.searchbarElem.nativeElement.children&&0!==this.searchbarElem.nativeElement.children.length&&this.searchbarElem.nativeElement.children[0].children&&0!==this.searchbarElem.nativeElement.children[0].children.length&&this.searchbarElem.nativeElement.children[0].children[0].value&&(this.searchbarElem.nativeElement.children[0].children[0].value=""))}_getPosition(e){let t=0,n=0;for(;e;){if("BODY"===e.tagName){const i=e.scrollLeft||document.documentElement.scrollLeft,o=e.scrollTop||document.documentElement.scrollTop;t+=e.offsetLeft-i+e.clientLeft,n+=e.offsetTop-o+e.clientTop}else t+=e.offsetLeft-e.scrollLeft+e.clientLeft,n+=e.offsetTop-e.scrollTop+e.clientTop;e=e.offsetParent}return{x:t,y:n}}isEventWithinElement(e,t){if(void 0===e)return!1;let n;return n=e instanceof u.Rgc?e.elementRef:e instanceof u.s_b?e.element.nativeElement:e,n&&n.nativeElement&&n.nativeElement.contains(t.target)}getFormValue(e){if(void 0===this.provider)return null;if(null==e||"function"==typeof this.provider)return null;let t=null==this.provider.formValueAttribute?this.provider.labelAttribute:this.provider.formValueAttribute;return"object"==typeof e&&t?e[t]:e}clickClear(){this.clearValue(),this.hideItemList(),this.itemsCleared.emit(!0)}clearValue(){this.keyword="",this.selection=null,this.formValue=null,this.focusedOption>0&&(this.focusedOption=this.focusedOption-1)}getItems(e,t){this.isLoading=!0,this.promise&&clearTimeout(this.promise),this.promise=setTimeout(()=>{let n;e&&(this.keyword=e.detail.target.value),this.showResultsFirst&&""===this.keyword.trim()&&(this.keyword=""),"function"==typeof this.provider?(n=this.provider(this.keyword),this.setSuggestions(n,t),this.isLoading=!1):(n=this.provider.getResults(this.keyword),n instanceof h.xQ?n=n.asObservable():n instanceof Promise&&(n=(0,d.D)(n)),n instanceof p.y?n.pipe((0,m.x)(()=>{this.isLoading=!1})).subscribe(e=>{this.setSuggestions(e,t)},e=>console.error(e)):(this.setSuggestions(n,t),this.isLoading=!1)),this.ionAutoInput.emit(this.keyword)},this.autocompleteOptions.debounce)}getLabel(e){if(void 0===this.provider)return"";if(null==e||"function"==typeof this.provider)return"";let t=null==this.provider.formValueAttribute?this.provider.labelAttribute:this.provider.formValueAttribute,n=e;if(this.provider.getItemLabel&&(n=this.provider.getItemLabel(n)),!this.multi&&void 0!==n&&"[object Array]"===Object.prototype.toString.call(n)){if(0===n.length)return"";n=n[0]}return"object"==typeof n&&t?n[t]||"":n||""}listStyles(){const e=this.listLocationStyles();return Object.assign(Object.assign({},e),this.styles.list)}listLocationStyles(){let e=this.location;if("auto"===this.location){const t=this._getPosition(this.searchbarElem.nativeElement).y;e=t>window.innerHeight-t?"top":"bottom"}return"bottom"===e?{}:{bottom:"37px"}}handleTabOut(e){this.selectOnTabOut&&0!==this.suggestions.length&&-1!==this.focusedOption?this.selectItem(this.suggestions[this.focusedOption]):this.hideItemList(),this.onBlur(e)}handleTap(e){(this.showResultsFirst||this.keyword.length>0)&&this.getItems()}handleSelectTap(e,t){return void 0!==t&&(this.selectItem(t),e.srcEvent?(e.srcEvent.stopPropagation&&e.srcEvent.stopPropagation(),e.srcEvent.preventDefault&&e.srcEvent.preventDefault()):e.preventDefault&&e.preventDefault()),!1}hideItemList(){!1===this.showSuggestions&&!1===this.alwaysShowList&&(this.showListChanged=!0),this.showSuggestions=this.alwaysShowList,this.focusedOption=-1}highlightItem(e){!1===this.showSuggestions&&this.showItemList();let t=this.suggestions.length-1;t>this.maxResults&&(t=this.maxResults-1),e<0?-1===this.focusedOption||this.focusedOption===t?this.focusedOption=0:this.focusedOption++:e>0&&(-1===this.focusedOption||0===this.focusedOption?this.focusedOption=t:this.focusedOption--)}onFocus(e){this.hasFocus=!0,this.getItems(),e=this._reflectName(e),this.autoFocus.emit(e),this.focus.emit(e)}onBlur(e){this.hasFocus=!1,e=this._reflectName(e),this.autoBlur.emit(e),this.blur.emit(e)}_reflectName(e){return"object"==typeof e.srcElement.attributes["ng-reflect-name"]&&(e.srcElement.name=e.srcElement.attributes["ng-reflect-name"].value),e}registerOnChange(e){this.onChangeCallback=e}registerOnTouched(e){this.onTouchedCallback=e}removeDuplicates(e){const t=this.selected?this.selected.length:0,n=e.length;for(let i=0;i<t;i++){const t=this.getLabel(this.selected[i]);for(let i=0;i<n;i++)t===this.getLabel(e[i])&&e.splice(i,1)}return e}removeExcluded(e){const t=this.exclude.length;for(let n=0;n<t;n++){let t=this.exclude[n];"object"==typeof t&&(t=this.getLabel(t));const i=e.length;for(let n=0;n<i;n++)if(t===this.getLabel(e[n])){e.splice(n,1);break}}return e}removeItem(e,t){const n=this.selected?this.selected.length:0;for(let i=0;i<n;i++){const t=this.selected[i];this.getLabel(e)===this.getLabel(t)&&this.selected.splice(i,1)}(t=void 0===t||t)&&(this.itemRemoved.emit(e),this.itemsChange.emit(this.selected)),this.modelChange.emit(this.selected)}selectItem(e){if(this.keyword=this.getLabel(e),this.formValue=this.getFormValue(e),this.updateModel(this.formValue),this.hideListOnSelection&&this.hideItemList(),this.multi){if(!(null===this.maxSelected||this.selected.length<=this.maxSelected))return;this.clearValue(),this.selected.push(e),this.itemsChange.emit(this.selected)}else this.selection=e,this.selected=[e],this.itemsChange.emit(e);this.itemSelected.emit(e),this.modelChange.emit(this.selected)}setFocus(){this.useIonInput&&this.inputElem?this.inputElem.nativeElement.setFocus():this.searchbarElem&&this.searchbarElem.nativeElement.setFocus()}setSuggestions(e,t){this.removeDuplicateSuggestions&&(e=this.removeDuplicates(e),e=this.removeExcluded(e)),this.suggestions=e,(t||void 0===t)&&this.showItemList(),this.autoFocusSuggestion&&0!==this.suggestions.length&&(this.focusedOption=0)}setValue(e){this.formValue=this.getFormValue(e),this.keyword=this.getLabel(e)}showItemList(){!1===this.showSuggestions&&(this.showListChanged=!0),this.showSuggestions=!0}updateModel(e){e!==this.formValue&&(this.formValue=e,this.multi||(this.selected=null)),this.onChangeCallback&&this.onChangeCallback(this.formValue),this.modelChange.emit(this.selected)}writeValue(e){e!==this.selection&&(this.selection=e||null,this.formValue=this.getFormValue(this.selection),this.keyword=this.getLabel(this.selection))}}return e.\u0275fac=function(t){return new(t||e)},e.\u0275cmp=u.Xpm({type:e,selectors:[["ion-auto-complete"]],viewQuery:function(e,t){if(1&e&&(u.Gf(g,5,u.SBq),u.Gf(f,5,u.SBq),u.Gf(b,5,u.SBq)),2&e){let e;u.iGM(e=u.CRH())&&(t.searchbarElem=e.first),u.iGM(e=u.CRH())&&(t.inputElem=e.first),u.iGM(e=u.CRH())&&(t.itemList=e.first)}},hostBindings:function(e,t){1&e&&u.NdJ("click",function(e){return t.documentClickHandler(e)},!1,u.evT)},inputs:{alwaysShowList:"alwaysShowList",autoFocusSuggestion:"autoFocusSuggestion",clearInvalidInput:"clearInvalidInput",disabled:"disabled",exclude:"exclude",frontIcon:"frontIcon",hideListOnSelection:"hideListOnSelection",label:"label",labelPosition:"labelPosition",location:"location",maxResults:"maxResults",maxSelected:"maxSelected",multi:"multi",name:"name",removeButtonClasses:"removeButtonClasses",removeButtonColor:"removeButtonColor",removeButtonIcon:"removeButtonIcon",removeButtonSlot:"removeButtonSlot",removeDuplicateSuggestions:"removeDuplicateSuggestions",selectOnTabOut:"selectOnTabOut",styles:"styles",useIonInput:"useIonInput",keyword:"keyword",dataProvider:"dataProvider",provider:"provider",eager:"eager",options:"options",autocomplete:"autocomplete",model:"model",showList:"showList",emptyTemplate:"emptyTemplate",listTemplate:"listTemplate",selectionTemplate:"selectionTemplate",showResultsFirst:"showResultsFirst",template:"template"},outputs:{autoBlur:"autoBlur",autoFocus:"autoFocus",blur:"blur",focus:"focus",ionAutoInput:"ionAutoInput",itemsChange:"itemsChange",itemsCleared:"itemsCleared",itemsHidden:"itemsHidden",itemRemoved:"itemRemoved",itemSelected:"itemSelected",itemsShown:"itemsShown",modelChange:"modelChange"},features:[u._Bn([{provide:o.JU,useExisting:e,multi:!0}])],decls:15,vars:50,consts:[["defaultSelection",""],[4,"ngIf"],[3,"position",4,"ngIf"],[3,"autocomplete","name","ngModel","placeholder","type","clearOnEdit","clearInput","color","mode","disabled","ngClass","ngStyle","ionInput","tap","ngModelChange","keydown.tab","keydown.shift.tab","keyup.arrowDown","keyup.arrowUp","keyup.enter","keyup.escape","ionFocus","ionBlur"],["inputElem",""],[3,"autocomplete","name","animated","ngModel","cancelButtonIcon","cancelButtonText","clearIcon","color","showCancelButton","debounce","placeholder","autocorrect","mode","searchIcon","spellcheck","type","ngClass","ngStyle","ionInput","tap","ngModelChange","keydown.tab","keydown.shift.tab","keyup.arrowDown","keyup.arrowUp","keyup.enter","keyup.escape","ionClear","ionFocus","ionBlur"],["searchbarElem",""],["defaultTemplate",""],["class","ion-text-center"],["defaultEmptyTemplate",""],[3,"ngTemplateOutlet","ngTemplateOutletContext"],["defaultList",""],[3,"color","outline"],["color","primary",3,"name","slot",4,"ngIf"],[3,"name","slot",4,"ngIf"],["color","primary",3,"name","slot"],[3,"name","slot"],["class","selected-items",3,"click",4,"ngFor","ngForOf"],[1,"selected-items",3,"click"],[3,"position"],[3,"innerHTML"],[3,"ngStyle",4,"ngIf"],[3,"ngStyle"],["itemList",""],[3,"ngClass","ngStyle","mouseenter","click","tap",4,"ngFor","ngForOf"],[3,"ngClass","ngStyle","mouseenter","click","tap"]],template:function(e,t){if(1&e&&(u.YNc(0,O,5,8,"ng-template",null,0,u.W1O),u.YNc(2,k,2,1,"div",1),u.YNc(3,S,2,2,"ion-label",2),u.TgZ(4,"ion-input",3,4),u.NdJ("ionInput",function(e){return t.getItems(e)})("tap",function(e){return t.handleTap(e)})("ngModelChange",function(e){return t.keyword=e})("ngModelChange",function(e){return t.updateModel(e)})("keydown.tab",function(e){return t.handleTabOut(e)})("keydown.shift.tab",function(){return t.hideItemList()})("keyup.arrowDown",function(){return t.highlightItem(-1)})("keyup.arrowUp",function(){return t.highlightItem(1)})("keyup.enter",function(e){return t.handleSelectTap(e,t.suggestions[t.focusedOption])})("keyup.escape",function(){return t.hideItemList()})("ionFocus",function(e){return t.onFocus(e)})("ionBlur",function(e){return t.onBlur(e)}),u.qZA(),u.TgZ(6,"ion-searchbar",5,6),u.NdJ("ionInput",function(e){return t.getItems(e)})("tap",function(e){return t.handleTap(e)})("ngModelChange",function(e){return t.keyword=e})("ngModelChange",function(e){return t.updateModel(e)})("keydown.tab",function(e){return t.handleTabOut(e)})("keydown.shift.tab",function(){return t.hideItemList()})("keyup.arrowDown",function(){return t.highlightItem(-1)})("keyup.arrowUp",function(){return t.highlightItem(1)})("keyup.enter",function(e){return t.handleSelectTap(e,t.suggestions[t.focusedOption])})("keyup.escape",function(){return t.hideItemList()})("ionClear",function(){return t.clickClear()})("ionFocus",function(e){return t.onFocus(e)})("ionBlur",function(e){return t.onBlur(e)}),u.qZA(),u.YNc(8,Z,2,4,"ng-template",null,7,u.W1O),u.YNc(10,A,1,1,"ng-template",8,9,u.W1O),u.YNc(12,T,0,0,"ng-template",10),u.YNc(13,V,2,2,"ng-template",null,11,u.W1O)),2&e){const e=u.MAs(14);u.xp6(2),u.Q6J("ngIf",t.multi),u.xp6(1),u.Q6J("ngIf",0!==t.label.length),u.xp6(1),u.Q6J("autocomplete",t.autocomplete)("name",t.name)("ngModel",t.keyword)("placeholder",null==t.autocompleteOptions.placeholder?t.defaultOpts.placeholder:t.autocompleteOptions.placeholder)("type",null==t.autocompleteOptions.type?t.defaultOpts.type:t.autocompleteOptions.type)("clearOnEdit",null==t.autocompleteOptions.clearOnEdit?t.defaultOpts.clearOnEdit:t.autocompleteOptions.clearOnEdit)("clearInput",null==t.autocompleteOptions.clearInput?t.defaultOpts.clearInput:t.autocompleteOptions.clearInput)("color",null==t.autocompleteOptions.color?null:t.autocompleteOptions.color)("mode",null==t.autocompleteOptions.mode?t.defaultOpts.mode:t.autocompleteOptions.mode)("disabled",t.disabled||null!==t.maxSelected&&t.selected.length>=t.maxSelected)("ngClass",u.WLB(34,F,!t.useIonInput,t.isLoading))("ngStyle",t.styles.searchbar),u.xp6(2),u.Q6J("autocomplete",t.autocomplete)("name",t.name)("animated",null==t.autocompleteOptions.animated?t.defaultOpts.animated:t.autocompleteOptions.animated)("ngModel",t.keyword)("cancelButtonIcon",null==t.autocompleteOptions.cancelButtonIcon?t.defaultOpts.cancelButtonIcon:t.autocompleteOptions.cancelButtonIcon)("cancelButtonText",null==t.autocompleteOptions.cancelButtonText?t.defaultOpts.cancelButtonText:t.autocompleteOptions.cancelButtonText)("clearIcon",null==t.autocompleteOptions.clearIcon?t.defaultOpts.clearIcon:t.autocompleteOptions.clearIcon)("color",null==t.autocompleteOptions.color?null:t.autocompleteOptions.color)("showCancelButton",null==t.autocompleteOptions.showCancelButton?t.defaultOpts.showCancelButton?"always":"never":t.autocompleteOptions.showCancelButton?"always":"never")("debounce",null==t.autocompleteOptions.debounce?t.defaultOpts.debounce:t.autocompleteOptions.debounce)("placeholder",null==t.autocompleteOptions.placeholder?t.defaultOpts.placeholder:t.autocompleteOptions.placeholder)("autocorrect",null==t.autocompleteOptions.autocorrect?t.defaultOpts.autocorrect:t.autocompleteOptions.autocorrect)("mode",null==t.autocompleteOptions.mode?t.defaultOpts.mode:t.autocompleteOptions.mode)("searchIcon",null==t.autocompleteOptions.searchIcon?t.defaultOpts.searchIcon:t.autocompleteOptions.searchIcon)("spellcheck",null==t.autocompleteOptions.spellcheck?t.defaultOpts.spellcheck:t.autocompleteOptions.spellcheck)("type",null==t.autocompleteOptions.type?t.defaultOpts.type:t.autocompleteOptions.type)("ngClass",u.kEZ(37,R,t.useIonInput,t.isLoading,t.disabled||null!==t.maxSelected&&t.selected.length>=t.maxSelected))("ngStyle",t.styles.searchbar),u.xp6(6),u.Q6J("ngTemplateOutlet",t.listTemplate||e)("ngTemplateOutletContext",u.VKq(48,I,u.HTZ(41,N,t.suggestions,t.keyword,t.maxResults,t.maxSelected,t.selected,t.showSuggestions)))}},directives:function(){return[i.O5,l.pK,l.j9,o.JJ,o.On,i.mk,i.PC,l.VI,i.tP,l.hM,l.Q$,l.gu,i.sg]},pipes:function(){return[H,i.OU]},styles:["ion-auto-complete[_ngcontent-%COMP%]{overflow:hidden!important;width:90vw;display:inline-block}ion-auto-complete[_ngcontent-%COMP%]   ion-searchbar[_ngcontent-%COMP%]{padding:1px!important}ion-auto-complete[_ngcontent-%COMP%]   .disabled[_ngcontent-%COMP%]   input.searchbar-input[_ngcontent-%COMP%]{pointer-events:none;cursor:default}ion-auto-complete[_ngcontent-%COMP%]   ul[_ngcontent-%COMP%]{position:absolute;width:90vw;margin-top:0;background:#fff;list-style-type:none;padding:0;left:16px;z-index:999;box-shadow:0 2px 2px 0 rgba(0,0,0,.14),0 3px 1px -2px rgba(0,0,0,.2),0 1px 5px 0 rgba(0,0,0,.12)}ion-auto-complete[_ngcontent-%COMP%]   ul[_ngcontent-%COMP%]   li[_ngcontent-%COMP%]{padding:15px;border-bottom:1px solid #c1c1c1}ion-auto-complete[_ngcontent-%COMP%]   ul[_ngcontent-%COMP%]   li[_ngcontent-%COMP%]   span[_ngcontent-%COMP%]{pointer-events:none}ion-auto-complete[_ngcontent-%COMP%]   ul[_ngcontent-%COMP%]   ion-auto-complete-item[_ngcontent-%COMP%]{height:40px;width:100%}ion-auto-complete[_ngcontent-%COMP%]   ul[_ngcontent-%COMP%]   li[_ngcontent-%COMP%]:last-child{border:none}ion-auto-complete[_ngcontent-%COMP%]   ul[_ngcontent-%COMP%]   li.focus[_ngcontent-%COMP%], ion-auto-complete[_ngcontent-%COMP%]   ul[_ngcontent-%COMP%]   li[_ngcontent-%COMP%]:focus{cursor:pointer;background:#f1f1f1}ion-auto-complete[_ngcontent-%COMP%]   .hidden[_ngcontent-%COMP%]{display:none}ion-auto-complete[_ngcontent-%COMP%]   .loading[_ngcontent-%COMP%]   input.searchbar-input[_ngcontent-%COMP%]{background:#fff url(/assets/loading.gif) no-repeat right 4px center;background-size:25px 25px}ion-auto-complete[_ngcontent-%COMP%]   .searchbar-clear-button.sc-ion-searchbar-md[_ngcontent-%COMP%]{right:34px}ion-auto-complete[_ngcontent-%COMP%]   .selected-items[_ngcontent-%COMP%]{float:left}"]}),e})(),H=(()=>{class e{transform(e,t){if(!t)return e;let n=t.replace(/[.*+?^${}()|[\]\\]/g,"\\$&");return e.replace(new RegExp(n,"gi"),function(e){return e.bold()})}}return e.\u0275fac=function(t){return new(t||e)},e.\u0275pipe=u.Yjl({name:"boldprefix",type:e,pure:!0}),e.\u0275prov=u.Yz7({token:e,factory:e.\u0275fac}),e})(),W=(()=>{class e{static forRoot(){return{ngModule:e,providers:[]}}}return e.\u0275fac=function(t){return new(t||e)},e.\u0275mod=u.oAB({type:e}),e.\u0275inj=u.cJS({imports:[[i.ez,o.u5,l.Pc]]}),e})();var D=n(600);function j(e,t){1&e&&(u.TgZ(0,"ion-item-divider",11),u._uU(1,"Vorbelegung des Vereinsnamens"),u.qZA())}function z(e,t){if(1&e){const e=u.EpF();u.TgZ(0,"ion-item",25),u.TgZ(1,"ion-auto-complete",26),u.NdJ("itemSelected",function(t){return u.CHM(e),u.oxw(2).vereinSelected(t)}),u.qZA(),u.qZA()}if(2&e){const e=u.oxw(2);u.xp6(1),u.Q6J("options",e.options)("dataProvider",e.provider)}}function K(e,t){if(1&e&&(u.TgZ(0,"ion-item"),u.TgZ(1,"ion-avatar",0),u._UZ(2,"img",27),u.qZA(),u._UZ(3,"ion-input",28),u._UZ(4,"ion-input",29),u.qZA()),2&e){const e=u.oxw(2);u.xp6(3),u.Q6J("readonly",!1)("disabled",!1)("ngModel",e.newRegistration.secret),u.xp6(1),u.Q6J("readonly",!1)("disabled",!1)("ngModel",e.newRegistration.verification)}}function $(e,t){if(1&e){const e=u.EpF();u.TgZ(0,"ion-item"),u.TgZ(1,"ion-avatar",0),u._UZ(2,"img",27),u.qZA(),u.TgZ(3,"ion-input",30),u.NdJ("ngModelChange",function(t){return u.CHM(e),u.oxw(2).changePassword.secret=t}),u.qZA(),u.TgZ(4,"ion-input",31),u.NdJ("ngModelChange",function(t){return u.CHM(e),u.oxw(2).changePassword.verification=t}),u.qZA(),u.qZA()}if(2&e){const e=u.oxw(2);u.xp6(3),u.Q6J("readonly",!1)("disabled",!1)("ngModel",e.changePassword.secret),u.xp6(1),u.Q6J("readonly",!1)("disabled",!1)("ngModel",e.changePassword.verification)}}function G(e,t){if(1&e){const e=u.EpF();u.TgZ(0,"ion-item"),u.TgZ(1,"ion-avatar",0),u._uU(2," \xa0 "),u.qZA(),u.TgZ(3,"ion-button",32,33),u.NdJ("click",function(){return u.CHM(e),u.oxw(2).savePWChange()}),u._UZ(5,"ion-icon",23),u._uU(6,"Passwort \xe4ndern "),u.qZA(),u.qZA()}if(2&e){const e=u.oxw(2);u.xp6(3),u.Q6J("disabled",e.waiting||!e.changePassword.secret||e.changePassword.secret!==e.changePassword.verification)}}function X(e,t){if(1&e&&(u.TgZ(0,"ion-item"),u._uU(1),u.qZA()),2&e){const e=t.$implicit;u.xp6(1),u.hij(" ",e," ")}}function ee(e,t){if(1&e){const e=u.EpF();u.TgZ(0,"ion-content"),u.YNc(1,j,2,0,"ion-item-divider",7),u.YNc(2,z,2,2,"ion-item",8),u.TgZ(3,"form",9,10),u.NdJ("ngSubmit",function(){u.CHM(e);const t=u.MAs(4);return u.oxw().save(t.value)})("keyup.enter",function(){u.CHM(e);const t=u.MAs(4);return u.oxw().save(t.value)}),u.TgZ(5,"ion-list"),u.TgZ(6,"ion-item-divider",11),u._uU(7,"Verein"),u.qZA(),u.TgZ(8,"ion-item"),u.TgZ(9,"ion-avatar",0),u._UZ(10,"img",12),u.qZA(),u._UZ(11,"ion-input",13),u._UZ(12,"ion-input",14),u.qZA(),u.TgZ(13,"ion-item-divider",11),u._uU(14,"Verantwortliche Person"),u.qZA(),u.TgZ(15,"ion-item"),u.TgZ(16,"ion-avatar",0),u._UZ(17,"img",15),u.qZA(),u._UZ(18,"ion-input",16),u._UZ(19,"ion-input",17),u.qZA(),u.YNc(20,K,5,6,"ion-item",4),u.YNc(21,$,5,6,"ion-item",4),u.YNc(22,G,7,1,"ion-item",4),u.TgZ(23,"ion-item-divider",11),u._uU(24,"Kontakt"),u.qZA(),u.TgZ(25,"ion-item"),u.TgZ(26,"ion-avatar",0),u._UZ(27,"img",18),u.qZA(),u._UZ(28,"ion-input",19),u._UZ(29,"ion-input",20),u.qZA(),u.qZA(),u.TgZ(30,"ion-button",21,22),u._UZ(32,"ion-icon",23),u._uU(33,"Speichern "),u.qZA(),u.qZA(),u.TgZ(34,"ion-list"),u.TgZ(35,"ion-item-divider",11),u._uU(36),u.qZA(),u.YNc(37,X,2,1,"ion-item",24),u.qZA(),u.qZA()}if(2&e){const e=u.MAs(4),t=u.oxw();u.xp6(1),u.Q6J("ngIf",0===t.regId),u.xp6(1),u.Q6J("ngIf",0===t.regId),u.xp6(9),u.Q6J("readonly",!1)("autofocus",!0)("disabled",!1)("ngModel",t.registration.vereinname),u.xp6(1),u.Q6J("readonly",!1)("disabled",!1)("ngModel",t.registration.verband),u.xp6(6),u.Q6J("readonly",!1)("disabled",!1)("ngModel",t.registration.respVorname),u.xp6(1),u.Q6J("readonly",!1)("disabled",!1)("ngModel",t.registration.respName),u.xp6(1),u.Q6J("ngIf",0===t.regId),u.xp6(1),u.Q6J("ngIf",t.regId>0),u.xp6(1),u.Q6J("ngIf",t.regId>0),u.xp6(6),u.Q6J("readonly",!1)("disabled",!1)("ngModel",t.registration.mail),u.xp6(1),u.Q6J("readonly",!1)("disabled",!1)("ngModel",t.registration.mobilephone),u.xp6(1),u.Q6J("disabled",t.waiting||!e.valid||e.untouched),u.xp6(6),u.hij("Pendente Verarbeitungen (",t.sSyncActions.length,")"),u.xp6(1),u.Q6J("ngForOf",t.sSyncActions)}}const te=[{path:"",component:(()=>{class e{constructor(e,t,n,i,o,s){this.navCtrl=e,this.route=t,this.backendService=n,this.alertCtrl=i,this.actionSheetController=o,this.zone=s,this.provider={getResults:e=>{const t=(e||"").trim(),n=[];return t&&this.clublist&&this.clublist.length>0&&this.clublist.forEach(e=>{this.filter(t)(e)&&n.push(e)}),n},getItemLabel:e=>e.name+" ("+e.verband+")"},this.waiting=!1,this.sSyncActions=[],this.clublist=[],this.backendService.competitions||this.backendService.getCompetitions(),this.backendService.getClubList().subscribe(e=>{this.clublist=e}),this.options=new U,this.options.autocomplete="on",this.options.autocorrect="off",this.options.noItems="Kein passender Vereinsname bekannt",this.options.placeholder="Vereinsname \xfcbernehmen ..."}vereinSelected(e){this.newRegistration.vereinname=e.name,this.newRegistration.verband=e.verband}ngOnInit(){this.waiting=!0,this.wkId=this.route.snapshot.paramMap.get("wkId"),this.regId=parseInt(this.route.snapshot.paramMap.get("regId")),this.backendService.getCompetitions().subscribe(e=>{this.wettkampfId=parseInt(e.find(e=>e.uuid===this.wkId).id),this.regId?this.backendService.getClubRegistrations(this.wkId).subscribe(e=>{e&&e.length>0&&this.updateUI(e.find(e=>e.id===this.regId))}):this.updateUI({mobilephone:"+417"})})}filter(e){const t=e.toUpperCase().split(" ");return n=>"*"===e.trim()||t.filter(e=>n.name.toUpperCase().indexOf(e)>-1||n.verband.toUpperCase().indexOf(e)>-1||void 0).length===t.length}presentActionSheet(){return(0,a.mG)(this,void 0,void 0,function*(){let e={};e=this.registration.vereinId?{header:"Anmeldungen",cssClass:"my-actionsheet-class",buttons:[{text:"Athlet & Athletinnen",icon:"list",handler:()=>{this.editAthletRegistrations()}},{text:"Wertungsrichter",icon:"glasses",handler:()=>{this.editJudgeRegistrations()}},{text:"Kopieren aus anderem Wettkampf",icon:"share",handler:()=>{this.copyFromOthers()}},{text:"Registrierung l\xf6schen",role:"destructive",icon:"trash",handler:()=>{this.delete()}},{text:"Abbrechen",icon:"close",role:"cancel",handler:()=>{console.log("Cancel clicked")}}]}:{header:"Anmeldungen",cssClass:"my-actionsheet-class",buttons:[{text:"Athlet & Athletinnen",icon:"list",handler:()=>{this.editAthletRegistrations()}},{text:"Wertungsrichter",icon:"glasses",handler:()=>{this.editJudgeRegistrations()}},{text:"Registrierung l\xf6schen",role:"destructive",icon:"trash",handler:()=>{this.delete()}},{text:"Abbrechen",icon:"close",role:"cancel",handler:()=>{console.log("Cancel clicked")}}]};const t=yield this.actionSheetController.create(e);yield t.present()})}editable(){return this.backendService.loggedIn}ionViewWillEnter(){this.getSyncActions()}getSyncActions(){this.regId>0&&this.backendService.loadRegistrationSyncActions().pipe((0,r.q)(1)).subscribe(e=>{this.sSyncActions=e.filter(e=>e.verein.id===this.registration.id).map(e=>e.caption)})}updateUI(e){this.zone.run(()=>{this.waiting=!1,this.wettkampf=this.backendService.competitionName,this.registration=e,0===this.regId?this.newRegistration=e:this.changePassword={id:this.regId,wettkampfId:e.wettkampfId||this.wettkampfId,secret:"",verification:""},this.registration.mail&&this.registration.mail.length>1&&(this.backendService.currentUserName=this.registration.mail)})}savePWChange(){this.changePassword&&this.changePassword.secret&&this.changePassword.secret===this.changePassword.verification&&this.backendService.saveClubRegistrationPW(this.wkId,this.changePassword)}save(e){if(0===this.regId){const t=e;t.secret!==t.verification?this.alertCtrl.create({header:"Achtung",subHeader:"Passwort-Verifikation",message:"Das Passwort stimmt nicht mit der Verifikation \xfcberein. Beide m\xfcssen genau gleich erfasst sein.",buttons:[{text:"OKAY",role:"cancel",handler:()=>{}}]}).then(e=>e.present()):this.backendService.createClubRegistration(this.wkId,{mail:t.mail,mobilephone:t.mobilephone,respName:t.respName,respVorname:t.respVorname,verband:t.verband,vereinname:t.vereinname,wettkampfId:t.wettkampfId||this.wettkampfId,secret:t.secret}).subscribe(e=>{this.regId=e.id,this.registration=e,this.backendService.currentUserName=e.mail,this.presentActionSheet()})}else this.backendService.saveClubRegistration(this.wkId,{id:this.registration.id,registrationTime:this.registration.registrationTime,vereinId:this.registration.vereinId,mail:e.mail,mobilephone:e.mobilephone,respName:e.respName,respVorname:e.respVorname,verband:e.verband,vereinname:e.vereinname,wettkampfId:e.wettkampfId||this.wettkampfId})}delete(){this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Vereins-Registrierung und dessen Anmeldungen am Wettkampf",message:"Hiermit wird die Vereinsanmeldung am Wettkampf komplett gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.backendService.deleteClubRegistration(this.wkId,this.regId).subscribe(()=>{this.navCtrl.pop()})}}]}).then(e=>e.present())}editAthletRegistrations(){this.navCtrl.navigateForward(`reg-athletlist/${this.backendService.competition}/${this.regId}`)}editJudgeRegistrations(){this.navCtrl.navigateForward(`reg-judgelist/${this.backendService.competition}/${this.regId}`)}copyFromOthers(){this.backendService.findCompetitionsByVerein(this.registration.vereinId).subscribe(e=>{const t=e.map(e=>({type:"radio",label:e.titel+" ("+(0,c.tC)(e.datum)+")",value:e}));this.alertCtrl.create({header:"Anmeldungen kopieren",cssClass:"my-optionselection-class",message:"Aus welchem Wettkampf sollen die Anmeldungen kopiert werden?",inputs:t,buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:e=>{this.backendService.copyClubRegsFromCompetition(e,this.backendService.competition,this.registration.id).subscribe(()=>{this.getSyncActions(),this.alertCtrl.create({header:"Anmeldungen kopieren",message:"Alle nicht bereits erfassten Anmeldungen wurden \xfcbernommen.",buttons:[{text:"OKAY",role:"cancel",handler:()=>{}},{text:"> Athlet-Anmeldungen",handler:()=>{this.editAthletRegistrations()}},{text:"> WR Anmeldungen",handler:()=>{this.editJudgeRegistrations()}}]}).then(e=>e.present())})}}]}).then(e=>e.present())})}}return e.\u0275fac=function(t){return new(t||e)(u.Y36(l.SH),u.Y36(s.gz),u.Y36(D.v),u.Y36(l.Br),u.Y36(l.BX),u.Y36(u.R0b))},e.\u0275cmp=u.Xpm({type:e,selectors:[["app-clubreg-editor"]],decls:15,vars:3,consts:[["slot","start"],["defaultHref","/"],["slot","end"],[1,"athlet"],[4,"ngIf"],["size","large","expand","block","color","primary",3,"disabled","click"],["slot","start","name","list"],["color","primary",4,"ngIf"],["style","overflow: inherit;",4,"ngIf"],[3,"ngSubmit","keyup.enter"],["clubRegistrationform","ngForm"],["color","primary"],["src","assets/imgs/verein.png"],["placeholder","Vereinsname","autocomplete","true","type","text","name","vereinname","required","",3,"readonly","autofocus","disabled","ngModel"],["placeholder","Verband","autocomplete","true","type","text","name","verband","required","",3,"readonly","disabled","ngModel"],["src","assets/imgs/chief.png"],["placeholder","Vorname","type","text","autocomplete","given-name","name","respVorname","required","",3,"readonly","disabled","ngModel"],["placeholder","Nachname","type","text","autocomplete","name","name","respName","required","",3,"readonly","disabled","ngModel"],["src","assets/imgs/atmail.png"],["placeholder","Mail","type","email","autocomplete","email","pattern","^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,5})$","name","mail","required","",3,"readonly","disabled","ngModel"],["placeholder","Mobile/SMS (+4179...)","type","tel","autocomplete","on","pattern","[+]{1}[0-9]{11,14}","name","mobilephone","required","",3,"readonly","disabled","ngModel"],["size","large","expand","block","type","submit","color","success",3,"disabled"],["btnSaveNext",""],["slot","start","name","save"],[4,"ngFor","ngForOf"],[2,"overflow","inherit"],[1,"suggester",3,"options","dataProvider","itemSelected"],["src","assets/imgs/password.png"],["placeholder","Passwort","type","password","name","secret","required","",3,"readonly","disabled","ngModel"],["placeholder","Passwort best\xe4tigen","type","password","name","verification","required","",3,"readonly","disabled","ngModel"],["placeholder","Passwort","type","password","name","secret",3,"readonly","disabled","ngModel","ngModelChange"],["placeholder","Passwort best\xe4tigen","type","password","name","verification",3,"readonly","disabled","ngModel","ngModelChange"],["size","small","color","danger",3,"disabled","click"],["btnChangePassword",""]],template:function(e,t){1&e&&(u.TgZ(0,"ion-header"),u.TgZ(1,"ion-toolbar"),u.TgZ(2,"ion-buttons",0),u._UZ(3,"ion-back-button",1),u.qZA(),u.TgZ(4,"ion-title"),u._uU(5,"Vereins-Registrierung"),u.qZA(),u.TgZ(6,"ion-note",2),u.TgZ(7,"div",3),u._uU(8),u.qZA(),u.qZA(),u.qZA(),u.qZA(),u.YNc(9,ee,38,27,"ion-content",4),u.TgZ(10,"ion-footer"),u.TgZ(11,"ion-toolbar"),u.TgZ(12,"ion-button",5),u.NdJ("click",function(){return t.presentActionSheet()}),u._UZ(13,"ion-icon",6),u._uU(14,"Anmeldungen... "),u.qZA(),u.qZA(),u.qZA()),2&e&&(u.xp6(8),u.hij("f\xfcr ",t.wettkampf,""),u.xp6(1),u.Q6J("ngIf",t.registration),u.xp6(3),u.Q6J("disabled",t.waiting||0===t.regId))},directives:[l.Gu,l.sr,l.Sm,l.oU,l.cs,l.wd,l.uN,i.O5,l.fr,l.YG,l.gu,l.W2,o._Y,o.JL,o.F,l.q_,l.rH,l.Ie,l.BJ,l.pK,l.j9,o.Q7,o.JJ,o.On,o.c5,i.sg,Y],styles:[""]}),e})()}];let ne=(()=>{class e{}return e.\u0275fac=function(t){return new(t||e)},e.\u0275mod=u.oAB({type:e}),e.\u0275inj=u.cJS({imports:[[i.ez,o.u5,l.Pc,W,s.Bz.forChild(te)]]}),e})()}}]);