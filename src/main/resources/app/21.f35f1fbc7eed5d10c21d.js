(window.webpackJsonp=window.webpackJsonp||[]).push([[21],{PLua:function(n,l,e){"use strict";e.r(l);var t=e("CcnG"),u=e("mrSG"),i=e("ZZ/e"),o=e("cygB"),r=e("Zr1d"),s=function(){function n(n,l,e,t,u,i,o,r,s){this.navCtrl=n,this.route=l,this.sanitizer=e,this.alertCtrl=t,this.toastController=u,this.keyboard=i,this.backendService=o,this.platform=r,this.zone=s,this.waiting=!1,this.isDNoteUsed=!0,this.durchgang=o.durchgang,this.step=o.step,this.geraetId=o.geraet;var a=parseInt(this.route.snapshot.paramMap.get("itemId"));this.updateUI(o.wertungen.find(function(n){return n.id===a})),this.isDNoteUsed=this.item.isDNoteUsed}return n.prototype.ionViewWillLeave=function(){this.subscription&&(this.subscription.unsubscribe(),this.subscription=void 0)},n.prototype.ionViewWillEnter=function(){var n=this;this.subscription&&(this.subscription.unsubscribe(),this.subscription=void 0),this.subscription=this.backendService.wertungUpdated.subscribe(function(l){console.log("incoming wertung from service",l),l.wertung.athletId===n.wertung.athletId&&l.wertung.wettkampfdisziplinId===n.wertung.wettkampfdisziplinId&&l.wertung.endnote!==n.wertung.endnote&&(console.log("updateing wertung from service"),n.item.wertung=Object.assign({},l.wertung),n.itemOriginal.wertung=Object.assign({},l.wertung),n.wertung=Object.assign({noteD:0,noteE:0,endnote:0},n.item.wertung))}),this.platform.ready().then(function(){setTimeout(function(){n.keyboard.show&&(n.keyboard.show(),console.log("keyboard called")),n.isDNoteUsed&&n.dnote?(n.dnote.setFocus(),console.log("dnote focused")):n.enote&&(n.enote.setFocus(),console.log("enote focused"))},400)})},n.prototype.editable=function(){return this.backendService.loggedIn},n.prototype.updateUI=function(n){var l=this;this.zone.run(function(){l.waiting=!1,l.item=Object.assign({},n),l.itemOriginal=Object.assign({},n),l.wertung=Object.assign({noteD:0,noteE:0,endnote:0},l.itemOriginal.wertung),l.ionViewWillEnter()})},n.prototype.ensureInitialValues=function(n){return Object.assign(this.wertung,n)},n.prototype.saveClose=function(n){var l=this;this.waiting=!0,this.backendService.updateWertung(this.durchgang,this.step,this.geraetId,this.ensureInitialValues(n.value)).subscribe(function(n){l.updateUI(n),l.navCtrl.pop()},function(n){l.updateUI(l.itemOriginal),console.log(n)})},n.prototype.save=function(n){var l=this;this.waiting=!0,this.backendService.updateWertung(this.durchgang,this.step,this.geraetId,this.ensureInitialValues(n.value)).subscribe(function(n){l.updateUI(n)},function(n){l.updateUI(l.itemOriginal),console.log(n)})},n.prototype.saveNext=function(n){var l=this;this.waiting=!0,this.backendService.updateWertung(this.durchgang,this.step,this.geraetId,this.ensureInitialValues(n.value)).subscribe(function(e){l.waiting=!1;var t=l.backendService.wertungen.findIndex(function(n){return n.wertung.id===e.wertung.id});t<0&&console.log("unexpected wertung - id matches not with current wertung: "+e.wertung.id);var u=t+1;if(t<0)u=0;else if(t>=l.backendService.wertungen.length-1){if(0===l.backendService.wertungen.filter(function(n){return void 0===n.wertung.endnote}).length)return l.navCtrl.pop(),void l.toastSuggestCompletnessCheck();u=l.backendService.wertungen.findIndex(function(n){return void 0===n.wertung.endnote}),l.toastMissingResult(n,l.backendService.wertungen[u].vorname+" "+l.backendService.wertungen[u].name)}n.resetForm(),l.updateUI(l.backendService.wertungen[u])},function(n){l.updateUI(l.itemOriginal),console.log(n)})},n.prototype.nextEmptyOrFinish=function(n){var l=this,e=this.backendService.wertungen.findIndex(function(n){return n.wertung.id===l.wertung.id});if(0===this.backendService.wertungen.filter(function(n,l){return l>e&&void 0===n.wertung.endnote}).length)return this.navCtrl.pop(),void this.toastSuggestCompletnessCheck();var t=this.backendService.wertungen.findIndex(function(n,l){return l>e&&void 0===n.wertung.endnote});this.toastMissingResult(n,this.backendService.wertungen[t].vorname+" "+this.backendService.wertungen[t].name),n.resetForm(),this.updateUI(this.backendService.wertungen[t])},n.prototype.geraetName=function(){var n=this;return this.backendService.geraete?this.backendService.geraete.find(function(l){return l.id===n.geraetId}).name:""},n.prototype.toastSuggestCompletnessCheck=function(){return u.b(this,void 0,void 0,function(){return u.e(this,function(n){switch(n.label){case 0:return[4,this.toastController.create({header:"Qualit\xe4tskontrolle",message:"Es sind jetzt alle Resultate erfasst. Bitte ZU ZWEIT die Resultate pr\xfcfen und abschliessen!",animated:!0,position:"middle",buttons:[{text:"OK, gelesen.",icon:"checkmark-circle",role:"cancel",handler:function(){console.log("OK, gelesen. clicked")}}]})];case 1:return n.sent().present(),[2]}})})},n.prototype.toastMissingResult=function(n,l){return u.b(this,void 0,void 0,function(){var e=this;return u.e(this,function(t){switch(t.label){case 0:return[4,this.alertCtrl.create({header:"Achtung",subHeader:"Fehlendes Resultat!",message:"Nach der Erfassung der letzten Wertung in dieser Riege scheint es noch leere Wertungen zu geben. Bitte pr\xfcfen, ob "+l.toUpperCase()+" geturnt hat.",buttons:[{text:"Nicht geturnt",role:"edit",handler:function(){e.nextEmptyOrFinish(n)}},{text:"Korrigieren",role:"cancel",handler:function(){console.log("Korrigieren clicked")}}]})];case 1:return t.sent().present(),[2]}})})},n}(),a=function(){return function(){}}(),b=e("pMnS"),d=e("oBZk"),c=e("gIcY"),g=e("Ip0R"),h=e("ZYCi"),p=e("ZYjt"),m=t.nb({encapsulation:0,styles:[[".riege[_ngcontent-%COMP%]{font-size:small;padding-right:16px;color:var(--ion-color-medium)}.athlet[_ngcontent-%COMP%]{font-size:larger;padding-right:16px;font-weight:bolder;color:var(--ion-color-primary)}"]],data:{}});function f(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,4,"ion-button",[["color","success"],["expand","block"],["size","large"],["type","submit"]],null,null,null,d.I,d.d)),t.ob(1,49152,[["btnSaveNext",4]],0,i.j,[t.h,t.k],{color:[0,"color"],disabled:[1,"disabled"],expand:[2,"expand"],size:[3,"size"],type:[4,"type"]},null),(n()(),t.pb(2,0,null,0,1,"ion-icon",[["name","arrow-dropright-circle"],["slot","start"]],null,null,null,d.Q,d.l)),t.ob(3,49152,null,0,i.B,[t.h,t.k],{name:[0,"name"]},null),(n()(),t.Fb(-1,0,["Speichern & Weiter"]))],function(n,l){n(l,1,0,"success",l.component.waiting||!t.yb(l.parent,25).valid,"block","large","submit"),n(l,3,0,"arrow-dropright-circle")},null)}function v(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,4,"ion-button",[["color","secondary"],["expand","block"],["size","large"]],null,[[null,"click"]],function(n,l,e){var u=!0;return"click"===l&&(u=!1!==n.component.saveClose(t.yb(n.parent,25))&&u),u},d.I,d.d)),t.ob(1,49152,null,0,i.j,[t.h,t.k],{color:[0,"color"],disabled:[1,"disabled"],expand:[2,"expand"],size:[3,"size"]},null),(n()(),t.pb(2,0,null,0,1,"ion-icon",[["name","checkmark-circle"],["slot","start"]],null,null,null,d.Q,d.l)),t.ob(3,49152,null,0,i.B,[t.h,t.k],{name:[0,"name"]},null),(n()(),t.Fb(-1,0,["Speichern"]))],function(n,l){n(l,1,0,"secondary",l.component.waiting||!t.yb(l.parent,25).valid,"block","large"),n(l,3,0,"checkmark-circle")},null)}function w(n){return t.Hb(0,[t.Db(402653184,1,{form:0}),t.Db(402653184,2,{enote:0}),t.Db(402653184,3,{dnote:0}),(n()(),t.pb(3,0,null,null,17,"ion-header",[],null,null,null,d.P,d.k)),t.ob(4,49152,null,0,i.A,[t.h,t.k],null,null),(n()(),t.pb(5,0,null,0,15,"ion-toolbar",[],null,null,null,d.jb,d.E)),t.ob(6,49152,null,0,i.Ab,[t.h,t.k],null,null),(n()(),t.pb(7,0,null,0,4,"ion-buttons",[["slot","start"]],null,null,null,d.J,d.e)),t.ob(8,49152,null,0,i.k,[t.h,t.k],null,null),(n()(),t.pb(9,0,null,0,2,"ion-back-button",[["defaultHref","/"]],null,[[null,"click"]],function(n,l,e){var u=!0;return"click"===l&&(u=!1!==t.yb(n,11).onClick(e)&&u),u},d.H,d.c)),t.ob(10,49152,null,0,i.f,[t.h,t.k],{defaultHref:[0,"defaultHref"]},null),t.ob(11,16384,null,0,i.g,[[2,i.gb],i.Gb],{defaultHref:[0,"defaultHref"]},null),(n()(),t.pb(12,0,null,0,2,"ion-title",[],null,null,null,d.ib,d.D)),t.ob(13,49152,null,0,i.yb,[t.h,t.k],null,null),(n()(),t.Fb(14,0,["",""])),(n()(),t.pb(15,0,null,0,5,"ion-note",[["slot","end"]],null,null,null,d.bb,d.w)),t.ob(16,49152,null,0,i.W,[t.h,t.k],null,null),(n()(),t.pb(17,0,null,0,1,"div",[["class","athlet"]],null,null,null,null,null)),(n()(),t.Fb(18,null,["",""])),(n()(),t.pb(19,0,null,0,1,"div",[["class","riege"]],null,null,null,null,null)),(n()(),t.Fb(20,null,["",""])),(n()(),t.pb(21,0,null,null,52,"ion-content",[],null,null,null,d.M,d.h)),t.ob(22,49152,null,0,i.t,[t.h,t.k],null,null),(n()(),t.pb(23,0,null,0,50,"form",[["novalidate",""]],[[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ngSubmit"],[null,"keyup.enter"],[null,"submit"],[null,"reset"]],function(n,l,e){var u=!0,i=n.component;return"submit"===l&&(u=!1!==t.yb(n,25).onSubmit(e)&&u),"reset"===l&&(u=!1!==t.yb(n,25).onReset()&&u),"ngSubmit"===l&&(u=!1!==i.saveNext(t.yb(n,25))&&u),"keyup.enter"===l&&(u=!1!==i.saveNext(t.yb(n,25))&&u),u},null,null)),t.ob(24,16384,null,0,c.l,[],null,null),t.ob(25,4210688,[[1,4],["wertungsform",4]],0,c.h,[[8,null],[8,null]],null,{ngSubmit:"ngSubmit"}),t.Cb(2048,null,c.a,null,[c.h]),t.ob(27,16384,null,0,c.g,[[4,c.a]],null,null),(n()(),t.pb(28,0,null,null,39,"ion-list",[],null,null,null,d.Y,d.t)),t.ob(29,49152,null,0,i.N,[t.h,t.k],null,null),(n()(),t.pb(30,0,null,0,11,"ion-item",[],[[8,"hidden",0]],null,null,d.W,d.n)),t.ob(31,49152,null,0,i.G,[t.h,t.k],null,null),(n()(),t.pb(32,0,null,0,2,"ion-label",[],null,null,null,d.X,d.s)),t.ob(33,49152,null,0,i.M,[t.h,t.k],null,null),(n()(),t.Fb(-1,0,["D-Note"])),(n()(),t.pb(35,0,null,0,6,"ion-input",[["autofocus",""],["name","noteD"],["type","number"]],[[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ionBlur"],[null,"ionChange"]],function(n,l,e){var u=!0;return"ionBlur"===l&&(u=!1!==t.yb(n,36)._handleBlurEvent()&&u),"ionChange"===l&&(u=!1!==t.yb(n,36)._handleIonChange(e.target.value)&&u),u},d.R,d.m)),t.ob(36,16384,null,0,i.Hb,[t.k],null,null),t.Cb(1024,null,c.d,function(n){return[n]},[i.Hb]),t.ob(38,671744,null,0,c.i,[[2,c.a],[8,null],[8,null],[6,c.d]],{name:[0,"name"],isDisabled:[1,"isDisabled"],model:[2,"model"]},null),t.Cb(2048,null,c.e,null,[c.i]),t.ob(40,16384,null,0,c.f,[[4,c.e]],null,null),t.ob(41,49152,[[3,4],["dnote",4]],0,i.F,[t.h,t.k],{autofocus:[0,"autofocus"],disabled:[1,"disabled"],name:[2,"name"],readonly:[3,"readonly"],type:[4,"type"]},null),(n()(),t.pb(42,0,null,0,13,"ion-item",[],null,null,null,d.W,d.n)),t.ob(43,49152,null,0,i.G,[t.h,t.k],null,null),(n()(),t.pb(44,0,null,0,2,"ion-label",[],null,null,null,d.X,d.s)),t.ob(45,49152,null,0,i.M,[t.h,t.k],null,null),(n()(),t.Fb(-1,0,["E-Note"])),(n()(),t.pb(47,0,null,0,8,"ion-input",[["autofocus",""],["max","100"],["min","0"],["name","noteE"],["required",""],["step","0.01"],["type","number"]],[[1,"required",0],[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ionBlur"],[null,"ionChange"]],function(n,l,e){var u=!0;return"ionBlur"===l&&(u=!1!==t.yb(n,50)._handleBlurEvent()&&u),"ionChange"===l&&(u=!1!==t.yb(n,50)._handleIonChange(e.target.value)&&u),u},d.R,d.m)),t.ob(48,16384,null,0,c.j,[],{required:[0,"required"]},null),t.Cb(1024,null,c.c,function(n){return[n]},[c.j]),t.ob(50,16384,null,0,i.Hb,[t.k],null,null),t.Cb(1024,null,c.d,function(n){return[n]},[i.Hb]),t.ob(52,671744,null,0,c.i,[[2,c.a],[6,c.c],[8,null],[6,c.d]],{name:[0,"name"],isDisabled:[1,"isDisabled"],model:[2,"model"]},null),t.Cb(2048,null,c.e,null,[c.i]),t.ob(54,16384,null,0,c.f,[[4,c.e]],null,null),t.ob(55,49152,[[2,4],["enote",4]],0,i.F,[t.h,t.k],{autofocus:[0,"autofocus"],disabled:[1,"disabled"],max:[2,"max"],min:[3,"min"],name:[4,"name"],readonly:[5,"readonly"],required:[6,"required"],step:[7,"step"],type:[8,"type"]},null),(n()(),t.pb(56,0,null,0,11,"ion-item",[],null,null,null,d.W,d.n)),t.ob(57,49152,null,0,i.G,[t.h,t.k],null,null),(n()(),t.pb(58,0,null,0,2,"ion-label",[],null,null,null,d.X,d.s)),t.ob(59,49152,null,0,i.M,[t.h,t.k],null,null),(n()(),t.Fb(-1,0,["Endnote"])),(n()(),t.pb(61,0,null,0,6,"ion-input",[["name","endnote"],["readonly",""]],[[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ionBlur"],[null,"ionChange"]],function(n,l,e){var u=!0;return"ionBlur"===l&&(u=!1!==t.yb(n,62)._handleBlurEvent()&&u),"ionChange"===l&&(u=!1!==t.yb(n,62)._handleInputEvent(e.target.value)&&u),u},d.R,d.m)),t.ob(62,16384,null,0,i.Lb,[t.k],null,null),t.Cb(1024,null,c.d,function(n){return[n]},[i.Lb]),t.ob(64,671744,null,0,c.i,[[2,c.a],[8,null],[8,null],[6,c.d]],{name:[0,"name"],model:[1,"model"]},null),t.Cb(2048,null,c.e,null,[c.i]),t.ob(66,16384,null,0,c.f,[[4,c.e]],null,null),t.ob(67,49152,[["endnote",4]],0,i.F,[t.h,t.k],{name:[0,"name"],readonly:[1,"readonly"]},null),(n()(),t.pb(68,0,null,null,5,"ion-list",[],null,null,null,d.Y,d.t)),t.ob(69,49152,null,0,i.N,[t.h,t.k],null,null),(n()(),t.gb(16777216,null,0,1,null,f)),t.ob(71,16384,null,0,g.m,[t.O,t.L],{ngIf:[0,"ngIf"]},null),(n()(),t.gb(16777216,null,0,1,null,v)),t.ob(73,16384,null,0,g.m,[t.O,t.L],{ngIf:[0,"ngIf"]},null)],function(n,l){var e=l.component;n(l,10,0,"/"),n(l,11,0,"/"),n(l,38,0,"noteD",e.waiting,e.wertung.noteD),n(l,41,0,"",e.waiting,"noteD",!e.editable(),"number"),n(l,48,0,""),n(l,52,0,"noteE",e.waiting,e.wertung.noteE),n(l,55,0,"",e.waiting,"100","0","noteE",!e.editable(),"","0.01","number"),n(l,64,0,"endnote",e.wertung.endnote),n(l,67,0,"endnote",""),n(l,71,0,e.editable()),n(l,73,0,e.editable())},function(n,l){var e=l.component;n(l,14,0,e.geraetName()),n(l,18,0,e.item.vorname+" "+e.item.name),n(l,20,0,e.wertung.riege),n(l,23,0,t.yb(l,27).ngClassUntouched,t.yb(l,27).ngClassTouched,t.yb(l,27).ngClassPristine,t.yb(l,27).ngClassDirty,t.yb(l,27).ngClassValid,t.yb(l,27).ngClassInvalid,t.yb(l,27).ngClassPending),n(l,30,0,!e.isDNoteUsed),n(l,35,0,t.yb(l,40).ngClassUntouched,t.yb(l,40).ngClassTouched,t.yb(l,40).ngClassPristine,t.yb(l,40).ngClassDirty,t.yb(l,40).ngClassValid,t.yb(l,40).ngClassInvalid,t.yb(l,40).ngClassPending),n(l,47,0,t.yb(l,48).required?"":null,t.yb(l,54).ngClassUntouched,t.yb(l,54).ngClassTouched,t.yb(l,54).ngClassPristine,t.yb(l,54).ngClassDirty,t.yb(l,54).ngClassValid,t.yb(l,54).ngClassInvalid,t.yb(l,54).ngClassPending),n(l,61,0,t.yb(l,66).ngClassUntouched,t.yb(l,66).ngClassTouched,t.yb(l,66).ngClassPristine,t.yb(l,66).ngClassDirty,t.yb(l,66).ngClassValid,t.yb(l,66).ngClassInvalid,t.yb(l,66).ngClassPending)})}function y(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,1,"app-wertung-editor",[],null,null,null,w,m)),t.ob(1,49152,null,0,s,[i.Gb,h.a,p.b,i.a,i.Mb,r.a,o.a,i.Ib,t.z],null,null)],null,null)}var k=t.lb("app-wertung-editor",s,y,{},{},[]);e.d(l,"WertungEditorPageModuleNgFactory",function(){return C});var C=t.mb(a,[],function(n){return t.vb([t.wb(512,t.j,t.bb,[[8,[b.a,k]],[3,t.j],t.x]),t.wb(4608,g.o,g.n,[t.u,[2,g.u]]),t.wb(4608,c.m,c.m,[]),t.wb(4608,i.b,i.b,[t.z,t.g]),t.wb(4608,i.Fb,i.Fb,[i.b,t.j,t.q,g.d]),t.wb(4608,i.Jb,i.Jb,[i.b,t.j,t.q,g.d]),t.wb(4608,r.a,r.a,[]),t.wb(1073742336,g.c,g.c,[]),t.wb(1073742336,c.k,c.k,[]),t.wb(1073742336,c.b,c.b,[]),t.wb(1073742336,i.Cb,i.Cb,[]),t.wb(1073742336,h.n,h.n,[[2,h.t],[2,h.m]]),t.wb(1073742336,a,a,[]),t.wb(1024,h.k,function(){return[[{path:"",component:s}]]},[])])})}}]);