(window.webpackJsonp=window.webpackJsonp||[]).push([[20],{NzIK:function(l,n,e){"use strict";e.r(n);var u=e("CcnG"),t=e("ZZ/e"),i=e("cygB"),o=(e("ppBR"),function(){function l(l,n,e,u,t){this.navCtrl=l,this.route=n,this.backendService=e,this.alertCtrl=u,this.zone=t,this.waiting=!1}return l.prototype.ngOnInit=function(){var l=this;this.waiting=!0,this.wkId=this.route.snapshot.paramMap.get("wkId"),this.regId=parseInt(this.route.snapshot.paramMap.get("regId")),this.athletId=parseInt(this.route.snapshot.paramMap.get("athletId")),this.backendService.getCompetitions().subscribe(function(n){var e=n.find(function(n){return n.uuid===l.wkId});l.wettkampfId=parseInt(e.id),l.backendService.loadProgramsForCompetition(e.uuid).subscribe(function(n){l.wkPgms=n}),l.athletId?l.backendService.loadAthletRegistrations(l.wkId,l.regId).subscribe(function(n){l.updateUI(n.find(function(n){return n.id===l.athletId}))}):l.updateUI({id:0,vereinregistrationId:l.regId,name:"",vorname:"",geschlecht:"W",gebdat:void 0,programId:void 0,registrationTime:0})})},l.prototype.editable=function(){return this.backendService.loggedIn},l.prototype.updateUI=function(l){var n=this;this.zone.run(function(){n.waiting=!1,n.wettkampf=n.backendService.competitionName,n.registration=l})},l.prototype.save=function(l){var n=this,e=Object.assign({},this.registration,{gebdat:new Date(l.gebdat).toJSON()});0===this.athletId?this.backendService.createAthletRegistration(this.wkId,this.regId,e).subscribe(function(){n.navCtrl.pop()}):this.backendService.saveAthletRegistration(this.wkId,this.regId,e).subscribe(function(){n.navCtrl.pop()})},l.prototype.delete=function(){var l=this;this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Athlet-Anmeldung am Wettkampf",message:"Hiermit wird die Anmeldung von "+this.registration.name+", "+this.registration.vorname+" am Wettkampf gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:function(){}},{text:"OKAY",handler:function(){l.backendService.deleteAthletRegistration(l.wkId,l.regId,l.registration).subscribe(function(){l.navCtrl.pop()})}}]}).then(function(l){return l.present()})},l}()),a=function(){return function(){}}(),r=e("pMnS"),b=e("oBZk"),d=e("gIcY"),s=e("Ip0R"),g=e("ZYCi"),c=u.nb({encapsulation:0,styles:[[""]],data:{}});function h(l){return u.Hb(0,[(l()(),u.pb(0,0,null,null,2,"ion-select-option",[],null,null,null,b.eb,b.A)),u.ob(1,49152,null,0,t.mb,[u.h,u.k],{value:[0,"value"]},null),(l()(),u.Fb(2,0,["",""]))],function(l,n){l(n,1,0,n.context.$implicit.id)},function(l,n){l(n,2,0,n.context.$implicit.name)})}function p(l){return u.Hb(0,[(l()(),u.pb(0,0,null,null,98,"ion-content",[],null,null,null,b.M,b.h)),u.ob(1,49152,null,0,t.t,[u.h,u.k],null,null),(l()(),u.pb(2,0,null,0,96,"form",[["novalidate",""]],[[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ngSubmit"],[null,"keyup.enter"],[null,"submit"],[null,"reset"]],function(l,n,e){var t=!0,i=l.component;return"submit"===n&&(t=!1!==u.yb(l,4).onSubmit(e)&&t),"reset"===n&&(t=!1!==u.yb(l,4).onReset()&&t),"ngSubmit"===n&&(t=!1!==i.save(u.yb(l,4).value)&&t),"keyup.enter"===n&&(t=!1!==i.save(u.yb(l,4).value)&&t),t},null,null)),u.ob(3,16384,null,0,d.m,[],null,null),u.ob(4,4210688,[["athletRegistrationForm",4]],0,d.h,[[8,null],[8,null]],null,{ngSubmit:"ngSubmit"}),u.Cb(2048,null,d.a,null,[d.h]),u.ob(6,16384,null,0,d.g,[[4,d.a]],null,null),(l()(),u.pb(7,0,null,null,84,"ion-list",[],null,null,null,b.Y,b.t)),u.ob(8,49152,null,0,t.N,[u.h,u.k],null,null),(l()(),u.pb(9,0,null,0,2,"ion-item-divider",[["color","primary"]],null,null,null,b.S,b.o)),u.ob(10,49152,null,0,t.H,[u.h,u.k],{color:[0,"color"]},null),(l()(),u.Fb(-1,0,["Athlet / Athletin"])),(l()(),u.pb(12,0,null,0,22,"ion-item",[],null,null,null,b.W,b.n)),u.ob(13,49152,null,0,t.G,[u.h,u.k],null,null),(l()(),u.pb(14,0,null,0,2,"ion-avatar",[["slot","start"]],null,null,null,b.G,b.b)),u.ob(15,49152,null,0,t.e,[u.h,u.k],null,null),(l()(),u.pb(16,0,null,0,0,"img",[["src","assets/imgs/athlete.png"]],null,null,null,null,null)),(l()(),u.pb(17,0,null,0,8,"ion-input",[["name","name"],["placeholder","Name"],["required",""],["type","text"]],[[1,"required",0],[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ngModelChange"],[null,"ionBlur"],[null,"ionChange"]],function(l,n,e){var t=!0,i=l.component;return"ionBlur"===n&&(t=!1!==u.yb(l,20)._handleBlurEvent()&&t),"ionChange"===n&&(t=!1!==u.yb(l,20)._handleInputEvent(e.target.value)&&t),"ngModelChange"===n&&(t=!1!==(i.registration.name=e)&&t),t},b.R,b.m)),u.ob(18,16384,null,0,d.k,[],{required:[0,"required"]},null),u.Cb(1024,null,d.c,function(l){return[l]},[d.k]),u.ob(20,16384,null,0,t.Lb,[u.k],null,null),u.Cb(1024,null,d.d,function(l){return[l]},[t.Lb]),u.ob(22,671744,null,0,d.i,[[2,d.a],[6,d.c],[8,null],[6,d.d]],{name:[0,"name"],isDisabled:[1,"isDisabled"],model:[2,"model"]},{update:"ngModelChange"}),u.Cb(2048,null,d.e,null,[d.i]),u.ob(24,16384,null,0,d.f,[[4,d.e]],null,null),u.ob(25,49152,null,0,t.F,[u.h,u.k],{disabled:[0,"disabled"],name:[1,"name"],placeholder:[2,"placeholder"],readonly:[3,"readonly"],required:[4,"required"],type:[5,"type"]},null),(l()(),u.pb(26,0,null,0,8,"ion-input",[["name","vorname"],["placeholder","Vorname"],["required",""],["type","text"]],[[1,"required",0],[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ngModelChange"],[null,"ionBlur"],[null,"ionChange"]],function(l,n,e){var t=!0,i=l.component;return"ionBlur"===n&&(t=!1!==u.yb(l,29)._handleBlurEvent()&&t),"ionChange"===n&&(t=!1!==u.yb(l,29)._handleInputEvent(e.target.value)&&t),"ngModelChange"===n&&(t=!1!==(i.registration.vorname=e)&&t),t},b.R,b.m)),u.ob(27,16384,null,0,d.k,[],{required:[0,"required"]},null),u.Cb(1024,null,d.c,function(l){return[l]},[d.k]),u.ob(29,16384,null,0,t.Lb,[u.k],null,null),u.Cb(1024,null,d.d,function(l){return[l]},[t.Lb]),u.ob(31,671744,null,0,d.i,[[2,d.a],[6,d.c],[8,null],[6,d.d]],{name:[0,"name"],isDisabled:[1,"isDisabled"],model:[2,"model"]},{update:"ngModelChange"}),u.Cb(2048,null,d.e,null,[d.i]),u.ob(33,16384,null,0,d.f,[[4,d.e]],null,null),u.ob(34,49152,null,0,t.F,[u.h,u.k],{disabled:[0,"disabled"],name:[1,"name"],placeholder:[2,"placeholder"],readonly:[3,"readonly"],required:[4,"required"],type:[5,"type"]},null),(l()(),u.pb(35,0,null,0,16,"ion-item",[],null,null,null,b.W,b.n)),u.ob(36,49152,null,0,t.G,[u.h,u.k],null,null),(l()(),u.pb(37,0,null,0,2,"ion-avatar",[["slot","start"]],null,null,null,b.G,b.b)),u.ob(38,49152,null,0,t.e,[u.h,u.k],null,null),(l()(),u.Fb(-1,0,[" \xa0 "])),(l()(),u.pb(40,0,null,0,2,"ion-label",[["color","primary"]],null,null,null,b.X,b.s)),u.ob(41,49152,null,0,t.M,[u.h,u.k],{color:[0,"color"]},null),(l()(),u.Fb(-1,0,["Geburtsdatum"])),(l()(),u.pb(43,0,null,0,8,"ion-input",[["name","gebdat"],["placeholder","Geburtsdatum"],["required",""],["type","date"]],[[1,"required",0],[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ngModelChange"],[null,"ionBlur"],[null,"ionChange"]],function(l,n,e){var t=!0,i=l.component;return"ionBlur"===n&&(t=!1!==u.yb(l,46)._handleBlurEvent()&&t),"ionChange"===n&&(t=!1!==u.yb(l,46)._handleInputEvent(e.target.value)&&t),"ngModelChange"===n&&(t=!1!==(i.registration.gebdat=e)&&t),t},b.R,b.m)),u.ob(44,16384,null,0,d.k,[],{required:[0,"required"]},null),u.Cb(1024,null,d.c,function(l){return[l]},[d.k]),u.ob(46,16384,null,0,t.Lb,[u.k],null,null),u.Cb(1024,null,d.d,function(l){return[l]},[t.Lb]),u.ob(48,671744,null,0,d.i,[[2,d.a],[6,d.c],[8,null],[6,d.d]],{name:[0,"name"],isDisabled:[1,"isDisabled"],model:[2,"model"]},{update:"ngModelChange"}),u.Cb(2048,null,d.e,null,[d.i]),u.ob(50,16384,null,0,d.f,[[4,d.e]],null,null),u.ob(51,49152,null,0,t.F,[u.h,u.k],{disabled:[0,"disabled"],name:[1,"name"],placeholder:[2,"placeholder"],readonly:[3,"readonly"],required:[4,"required"],type:[5,"type"]},null),(l()(),u.pb(52,0,null,0,22,"ion-item",[],null,null,null,b.W,b.n)),u.ob(53,49152,null,0,t.G,[u.h,u.k],null,null),(l()(),u.pb(54,0,null,0,2,"ion-avatar",[["slot","start"]],null,null,null,b.G,b.b)),u.ob(55,49152,null,0,t.e,[u.h,u.k],null,null),(l()(),u.Fb(-1,0,[" \xa0 "])),(l()(),u.pb(57,0,null,0,2,"ion-label",[["color","primary"]],null,null,null,b.X,b.s)),u.ob(58,49152,null,0,t.M,[u.h,u.k],{color:[0,"color"]},null),(l()(),u.Fb(-1,0,["Geschlecht"])),(l()(),u.pb(60,0,null,0,14,"ion-select",[["cancelText","Abbrechen"],["name","geschlecht"],["okText","Okay"],["placeholder","Geschlecht"],["required",""]],[[1,"required",0],[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ngModelChange"],[null,"ionBlur"],[null,"ionChange"]],function(l,n,e){var t=!0,i=l.component;return"ionBlur"===n&&(t=!1!==u.yb(l,63)._handleBlurEvent()&&t),"ionChange"===n&&(t=!1!==u.yb(l,63)._handleChangeEvent(e.target.value)&&t),"ngModelChange"===n&&(t=!1!==(i.registration.geschlecht=e)&&t),t},b.fb,b.z)),u.ob(61,16384,null,0,d.k,[],{required:[0,"required"]},null),u.Cb(1024,null,d.c,function(l){return[l]},[d.k]),u.ob(63,16384,null,0,t.Kb,[u.k],null,null),u.Cb(1024,null,d.d,function(l){return[l]},[t.Kb]),u.ob(65,671744,null,0,d.i,[[2,d.a],[6,d.c],[8,null],[6,d.d]],{name:[0,"name"],model:[1,"model"]},{update:"ngModelChange"}),u.Cb(2048,null,d.e,null,[d.i]),u.ob(67,16384,null,0,d.f,[[4,d.e]],null,null),u.ob(68,49152,null,0,t.lb,[u.h,u.k],{cancelText:[0,"cancelText"],okText:[1,"okText"],placeholder:[2,"placeholder"],name:[3,"name"]},null),(l()(),u.pb(69,0,null,0,2,"ion-select-option",[],null,null,null,b.eb,b.A)),u.ob(70,49152,null,0,t.mb,[u.h,u.k],{value:[0,"value"]},null),(l()(),u.Fb(-1,0,["weiblich"])),(l()(),u.pb(72,0,null,0,2,"ion-select-option",[],null,null,null,b.eb,b.A)),u.ob(73,49152,null,0,t.mb,[u.h,u.k],{value:[0,"value"]},null),(l()(),u.Fb(-1,0,["m\xe4nnlich"])),(l()(),u.pb(75,0,null,0,2,"ion-item-divider",[["color","primary"]],null,null,null,b.S,b.o)),u.ob(76,49152,null,0,t.H,[u.h,u.k],{color:[0,"color"]},null),(l()(),u.Fb(-1,0,["Einteilung"])),(l()(),u.pb(78,0,null,0,13,"ion-item",[],null,null,null,b.W,b.n)),u.ob(79,49152,null,0,t.G,[u.h,u.k],null,null),(l()(),u.pb(80,0,null,0,2,"ion-avatar",[["slot","start"]],null,null,null,b.G,b.b)),u.ob(81,49152,null,0,t.e,[u.h,u.k],null,null),(l()(),u.pb(82,0,null,0,0,"img",[["src","assets/imgs/wettkampf.png"]],null,null,null,null,null)),(l()(),u.pb(83,0,null,0,8,"ion-select",[["cancelText","Abbrechen"],["name","programId"],["okText","Okay"],["placeholder","Programm/Kategorie"]],[[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ngModelChange"],[null,"ionBlur"],[null,"ionChange"]],function(l,n,e){var t=!0,i=l.component;return"ionBlur"===n&&(t=!1!==u.yb(l,84)._handleBlurEvent()&&t),"ionChange"===n&&(t=!1!==u.yb(l,84)._handleChangeEvent(e.target.value)&&t),"ngModelChange"===n&&(t=!1!==(i.registration.programId=e)&&t),t},b.fb,b.z)),u.ob(84,16384,null,0,t.Kb,[u.k],null,null),u.Cb(1024,null,d.d,function(l){return[l]},[t.Kb]),u.ob(86,671744,null,0,d.i,[[2,d.a],[8,null],[8,null],[6,d.d]],{name:[0,"name"],model:[1,"model"]},{update:"ngModelChange"}),u.Cb(2048,null,d.e,null,[d.i]),u.ob(88,16384,null,0,d.f,[[4,d.e]],null,null),u.ob(89,49152,null,0,t.lb,[u.h,u.k],{cancelText:[0,"cancelText"],okText:[1,"okText"],placeholder:[2,"placeholder"],name:[3,"name"]},null),(l()(),u.gb(16777216,null,0,1,null,h)),u.ob(91,278528,null,0,s.l,[u.O,u.L,u.s],{ngForOf:[0,"ngForOf"]},null),(l()(),u.pb(92,0,null,null,6,"ion-list",[],null,null,null,b.Y,b.t)),u.ob(93,49152,null,0,t.N,[u.h,u.k],null,null),(l()(),u.pb(94,0,null,0,4,"ion-button",[["color","success"],["expand","block"],["size","large"],["type","submit"]],null,null,null,b.I,b.d)),u.ob(95,49152,[["btnSaveNext",4]],0,t.j,[u.h,u.k],{color:[0,"color"],disabled:[1,"disabled"],expand:[2,"expand"],size:[3,"size"],type:[4,"type"]},null),(l()(),u.pb(96,0,null,0,1,"ion-icon",[["name",""],["slot","start"]],null,null,null,b.Q,b.l)),u.ob(97,49152,null,0,t.B,[u.h,u.k],{name:[0,"name"]},null),(l()(),u.Fb(-1,0,["Speichern"]))],function(l,n){var e=n.component;l(n,10,0,"primary"),l(n,18,0,""),l(n,22,0,"name",!1,e.registration.name),l(n,25,0,!1,"name","Name",!1,"","text"),l(n,27,0,""),l(n,31,0,"vorname",!1,e.registration.vorname),l(n,34,0,!1,"vorname","Vorname",!1,"","text"),l(n,41,0,"primary"),l(n,44,0,""),l(n,48,0,"gebdat",!1,e.registration.gebdat),l(n,51,0,!1,"gebdat","Geburtsdatum",!1,"","date"),l(n,58,0,"primary"),l(n,61,0,""),l(n,65,0,"geschlecht",e.registration.geschlecht),l(n,68,0,"Abbrechen","Okay","Geschlecht","geschlecht"),l(n,70,0,"W"),l(n,73,0,"M"),l(n,76,0,"primary"),l(n,86,0,"programId",e.registration.programId),l(n,89,0,"Abbrechen","Okay","Programm/Kategorie","programId"),l(n,91,0,e.wkPgms),l(n,95,0,"success",e.waiting||!u.yb(n,4).valid||u.yb(n,4).untouched,"block","large","submit"),l(n,97,0,"")},function(l,n){l(n,2,0,u.yb(n,6).ngClassUntouched,u.yb(n,6).ngClassTouched,u.yb(n,6).ngClassPristine,u.yb(n,6).ngClassDirty,u.yb(n,6).ngClassValid,u.yb(n,6).ngClassInvalid,u.yb(n,6).ngClassPending),l(n,17,0,u.yb(n,18).required?"":null,u.yb(n,24).ngClassUntouched,u.yb(n,24).ngClassTouched,u.yb(n,24).ngClassPristine,u.yb(n,24).ngClassDirty,u.yb(n,24).ngClassValid,u.yb(n,24).ngClassInvalid,u.yb(n,24).ngClassPending),l(n,26,0,u.yb(n,27).required?"":null,u.yb(n,33).ngClassUntouched,u.yb(n,33).ngClassTouched,u.yb(n,33).ngClassPristine,u.yb(n,33).ngClassDirty,u.yb(n,33).ngClassValid,u.yb(n,33).ngClassInvalid,u.yb(n,33).ngClassPending),l(n,43,0,u.yb(n,44).required?"":null,u.yb(n,50).ngClassUntouched,u.yb(n,50).ngClassTouched,u.yb(n,50).ngClassPristine,u.yb(n,50).ngClassDirty,u.yb(n,50).ngClassValid,u.yb(n,50).ngClassInvalid,u.yb(n,50).ngClassPending),l(n,60,0,u.yb(n,61).required?"":null,u.yb(n,67).ngClassUntouched,u.yb(n,67).ngClassTouched,u.yb(n,67).ngClassPristine,u.yb(n,67).ngClassDirty,u.yb(n,67).ngClassValid,u.yb(n,67).ngClassInvalid,u.yb(n,67).ngClassPending),l(n,83,0,u.yb(n,88).ngClassUntouched,u.yb(n,88).ngClassTouched,u.yb(n,88).ngClassPristine,u.yb(n,88).ngClassDirty,u.yb(n,88).ngClassValid,u.yb(n,88).ngClassInvalid,u.yb(n,88).ngClassPending)})}function m(l){return u.Hb(0,[(l()(),u.pb(0,0,null,null,4,"ion-button",[["color","danger"],["expand","block"],["size","large"]],null,[[null,"click"]],function(l,n,e){var u=!0;return"click"===n&&(u=!1!==l.component.delete()&&u),u},b.I,b.d)),u.ob(1,49152,[["btnDelete",4]],0,t.j,[u.h,u.k],{color:[0,"color"],disabled:[1,"disabled"],expand:[2,"expand"],size:[3,"size"]},null),(l()(),u.pb(2,0,null,0,1,"ion-icon",[["name","trash"],["slot","start"]],null,null,null,b.Q,b.l)),u.ob(3,49152,null,0,t.B,[u.h,u.k],{name:[0,"name"]},null),(l()(),u.Fb(-1,0,["Anmeldung l\xf6schen"]))],function(l,n){l(n,1,0,"danger",n.component.waiting,"block","large"),l(n,3,0,"trash")},null)}function y(l){return u.Hb(0,[(l()(),u.pb(0,0,null,null,15,"ion-header",[],null,null,null,b.P,b.k)),u.ob(1,49152,null,0,t.A,[u.h,u.k],null,null),(l()(),u.pb(2,0,null,0,13,"ion-toolbar",[],null,null,null,b.jb,b.E)),u.ob(3,49152,null,0,t.Ab,[u.h,u.k],null,null),(l()(),u.pb(4,0,null,0,4,"ion-buttons",[["slot","start"]],null,null,null,b.J,b.e)),u.ob(5,49152,null,0,t.k,[u.h,u.k],null,null),(l()(),u.pb(6,0,null,0,2,"ion-back-button",[["defaultHref","/"]],null,[[null,"click"]],function(l,n,e){var t=!0;return"click"===n&&(t=!1!==u.yb(l,8).onClick(e)&&t),t},b.H,b.c)),u.ob(7,49152,null,0,t.f,[u.h,u.k],{defaultHref:[0,"defaultHref"]},null),u.ob(8,16384,null,0,t.g,[[2,t.gb],t.Gb],{defaultHref:[0,"defaultHref"]},null),(l()(),u.pb(9,0,null,0,2,"ion-title",[],null,null,null,b.ib,b.D)),u.ob(10,49152,null,0,t.yb,[u.h,u.k],null,null),(l()(),u.Fb(-1,0,["Athlet-Anmeldung"])),(l()(),u.pb(12,0,null,0,3,"ion-note",[["slot","end"]],null,null,null,b.bb,b.w)),u.ob(13,49152,null,0,t.W,[u.h,u.k],null,null),(l()(),u.pb(14,0,null,0,1,"div",[["class","athlet"]],null,null,null,null,null)),(l()(),u.Fb(15,null,["f\xfcr ",""])),(l()(),u.gb(16777216,null,null,1,null,p)),u.ob(17,16384,null,0,s.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(l()(),u.pb(18,0,null,null,7,"ion-footer",[],null,null,null,b.N,b.i)),u.ob(19,49152,null,0,t.y,[u.h,u.k],null,null),(l()(),u.pb(20,0,null,0,5,"ion-toolbar",[],null,null,null,b.jb,b.E)),u.ob(21,49152,null,0,t.Ab,[u.h,u.k],null,null),(l()(),u.pb(22,0,null,0,3,"ion-list",[],null,null,null,b.Y,b.t)),u.ob(23,49152,null,0,t.N,[u.h,u.k],null,null),(l()(),u.gb(16777216,null,0,1,null,m)),u.ob(25,16384,null,0,s.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null)],function(l,n){var e=n.component;l(n,7,0,"/"),l(n,8,0,"/"),l(n,17,0,e.registration),l(n,25,0,e.athletId>0)},function(l,n){l(n,15,0,n.component.wettkampf)})}function k(l){return u.Hb(0,[(l()(),u.pb(0,0,null,null,1,"app-reg-athlet-editor",[],null,null,null,y,c)),u.ob(1,114688,null,0,o,[t.Gb,g.a,i.a,t.a,u.z],null,null)],function(l,n){l(n,1,0)},null)}var v=u.lb("app-reg-athlet-editor",o,k,{},{},[]);e.d(n,"RegAthletEditorPageModuleNgFactory",function(){return C});var C=u.mb(a,[],function(l){return u.vb([u.wb(512,u.j,u.bb,[[8,[r.a,v]],[3,u.j],u.x]),u.wb(4608,s.o,s.n,[u.u,[2,s.u]]),u.wb(4608,d.n,d.n,[]),u.wb(4608,t.b,t.b,[u.z,u.g]),u.wb(4608,t.Fb,t.Fb,[t.b,u.j,u.q,s.d]),u.wb(4608,t.Jb,t.Jb,[t.b,u.j,u.q,s.d]),u.wb(1073742336,s.c,s.c,[]),u.wb(1073742336,d.l,d.l,[]),u.wb(1073742336,d.b,d.b,[]),u.wb(1073742336,t.Cb,t.Cb,[]),u.wb(1073742336,g.n,g.n,[[2,g.t],[2,g.m]]),u.wb(1073742336,a,a,[]),u.wb(1024,g.k,function(){return[[{path:"",component:o}]]},[])])})}}]);