(window.webpackJsonp=window.webpackJsonp||[]).push([[16],{L6id:function(n,l,e){"use strict";e.r(l);var u=e("CcnG"),t=function(){return function(){}}(),o=e("pMnS"),i=e("oBZk"),r=e("ZZ/e"),c=e("Ip0R"),a=e("gIcY"),b=e("cygB"),g=e("Gyf/"),s=e("67Y/"),p=function(){function n(n,l,e){var u=this;this.navCtrl=n,this.backendService=l,this.alertCtrl=e,this.durchgangopen=!1,this.backendService.getCompetitions(),this.backendService.durchgangStarted.pipe(Object(s.a)(function(n){return n.filter(function(n){return Object(g.c)(n.durchgang)===Object(g.c)(u.backendService.durchgang)&&n.wettkampfUUID===u.backendService.competition}).length>0})).subscribe(function(n){u.durchgangstate=n?"gestartet":"gesperrt",u.durchgangopen=n})}return n.prototype.ngOnInit=function(){this.backendService.competition&&this.backendService.getDurchgaenge(this.backendService.competition)},Object.defineProperty(n.prototype,"isLocked",{get:function(){return this.backendService.stationFreezed},enumerable:!0,configurable:!0}),Object.defineProperty(n.prototype,"isOpenAndActive",{get:function(){return this.durchgangopen&&this.backendService.isWebsocketConnected()},enumerable:!0,configurable:!0}),Object.defineProperty(n.prototype,"isLoggedIn",{get:function(){return this.backendService.loggedIn},enumerable:!0,configurable:!0}),Object.defineProperty(n.prototype,"competition",{get:function(){return this.backendService.competition||""},set:function(n){this.isLocked||this.backendService.getDurchgaenge(n)},enumerable:!0,configurable:!0}),Object.defineProperty(n.prototype,"durchgang",{get:function(){return this.backendService.durchgang||""},set:function(n){this.isLocked||this.backendService.getGeraete(this.competition,n)},enumerable:!0,configurable:!0}),Object.defineProperty(n.prototype,"geraet",{get:function(){return this.backendService.geraet||-1},set:function(n){this.isLocked||this.backendService.getSteps(this.competition,this.durchgang,n)},enumerable:!0,configurable:!0}),Object.defineProperty(n.prototype,"step",{get:function(){return this.backendService.step||-1},set:function(n){this.backendService.getWertungen(this.competition,this.durchgang,this.geraet,n)},enumerable:!0,configurable:!0}),n.prototype.itemTapped=function(n){n.getOpenAmount().then(function(l){l>10||l<-10?n.close():n.open("end")})},n.prototype.nextStep=function(n){this.step=this.backendService.nextStep(),n.close()},n.prototype.prevStep=function(n){this.step=this.backendService.prevStep(),n.close()},n.prototype.getCompetitions=function(){return this.backendService.competitions},n.prototype.competitionName=function(){var n=this;if(!this.backendService.competitions)return"";var l=this.backendService.competitions.filter(function(l){return l.uuid===n.backendService.competition}).map(function(n){return n.titel+", am "+(n.datum+"T").split("T")[0].split("-").reverse().join("-")});return 1===l.length?l[0]:""},n.prototype.geraetName=function(){var n=this;if(!this.backendService.geraete)return"";var l=this.backendService.geraete.filter(function(l){return l.id===n.backendService.geraet}).map(function(n){return n.name});return 1===l.length?l[0]:""},n.prototype.getDurchgaenge=function(){return this.backendService.durchgaenge},n.prototype.getGeraete=function(){return this.backendService.geraete},n.prototype.getSteps=function(){return!this.backendService.steps&&this.backendService.geraet&&this.backendService.getSteps(this.competition,this.durchgang,this.geraet),this.backendService.steps},n.prototype.navToStation=function(){this.navCtrl.navigateForward("station")},n.prototype.unlock=function(){var n=this,l=this.competitionName()+"/"+this.durchgang+"/"+this.geraetName();this.alertCtrl.create({header:"Info",message:'Die Fixierung auf die Station "'+l+'" wird aufgehoben.<br>Du kannst dann die Station frei einstellen.',buttons:[{text:"ABBRECHEN",role:"cancel",handler:function(){}},{text:"OKAY",handler:function(){n.backendService.unlock()}}]}).then(function(n){return n.present()})},n.prototype.logout=function(){var n=this;this.alertCtrl.create({header:"Achtung",subHeader:"Nach dem Abmelden k\xf6nnen keine weiteren Resultate mehr erfasst werden.",message:"F\xfcr die Erfassung weiterer Resultate wird eine Neuanmeldung erforderlich.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:function(){}},{text:"OKAY",handler:function(){n.backendService.logout()}}]}).then(function(n){return n.present()})},n.prototype.finish=function(){var n=this,l=this.geraetName()+"/Riege #"+this.step,e=this.alertCtrl.create({header:"Achtung",message:'Nach dem Abschliessen der Station "'+l+'" k\xf6nnen die erfassten Wertungen nur noch im Wettkampf-B\xfcro korrigiert werden.',buttons:[{text:"ABBRECHEN",role:"cancel",handler:function(){}},{text:"OKAY",handler:function(){var l=e.then(function(n){return n.dismiss()});return n.backendService.finishStation(n.competition,n.durchgang,n.geraet,n.step).subscribe(function(e){0===e.length&&l.then(function(){n.navCtrl.pop()})}),!1}}]});e.then(function(n){return n.present()})},n}(),d=u.nb({encapsulation:2,styles:[],data:{}});function h(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,2,"ion-label",[],null,null,null,i.X,i.s)),u.ob(1,49152,null,0,r.M,[u.h,u.k],null,null),(n()(),u.Fb(2,0,[" ",", "," "]))],null,function(n,l){var e=l.component;n(l,2,0,e.durchgang,e.geraetName())})}function f(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,2,"ion-note",[["slot","end"]],null,null,null,i.bb,i.w)),u.ob(1,49152,null,0,r.W,[u.h,u.k],null,null),(n()(),u.Fb(2,0,[" "," "]))],null,function(n,l){n(l,2,0,"Riege "+l.component.step)})}function m(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,1,"ion-icon",[["name","arrow-dropright"],["slot","end"]],null,null,null,i.Q,i.l)),u.ob(1,49152,null,0,r.B,[u.h,u.k],{name:[0,"name"]},null)],function(n,l){n(l,1,0,"arrow-dropright")},null)}function k(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,35,"ion-list",[],null,null,null,i.Y,i.t)),u.ob(1,49152,null,0,r.N,[u.h,u.k],null,null),(n()(),u.pb(2,0,null,0,4,"ion-item",[],null,null,null,i.W,i.n)),u.ob(3,49152,null,0,r.G,[u.h,u.k],null,null),(n()(),u.pb(4,0,null,0,2,"ion-label",[],null,null,null,i.X,i.s)),u.ob(5,49152,null,0,r.M,[u.h,u.k],null,null),(n()(),u.Fb(6,0,["",""])),(n()(),u.pb(7,0,null,0,3,"ion-item",[],null,null,null,i.W,i.n)),u.ob(8,49152,null,0,r.G,[u.h,u.k],null,null),(n()(),u.gb(16777216,null,0,1,null,h)),u.ob(10,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(n()(),u.pb(11,0,null,0,24,"ion-item-sliding",[],null,null,null,i.V,i.r)),u.ob(12,49152,[["slidingStepItem",4]],0,r.L,[u.h,u.k],null,null),(n()(),u.pb(13,0,null,0,8,"ion-item",[],null,[[null,"click"]],function(n,l,e){var t=!0;return"click"===l&&(t=!1!==n.component.itemTapped(u.yb(n,12))&&t),t},i.W,i.n)),u.ob(14,49152,null,0,r.G,[u.h,u.k],null,null),(n()(),u.pb(15,0,null,0,2,"ion-label",[],null,null,null,i.X,i.s)),u.ob(16,49152,null,0,r.M,[u.h,u.k],null,null),(n()(),u.Fb(17,0,["",""])),(n()(),u.gb(16777216,null,0,1,null,f)),u.ob(19,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(n()(),u.gb(16777216,null,0,1,null,m)),u.ob(21,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(n()(),u.pb(22,0,null,0,6,"ion-item-options",[["side","start"]],null,null,null,i.U,i.q)),u.ob(23,49152,null,0,r.K,[u.h,u.k],{side:[0,"side"]},null),(n()(),u.pb(24,0,null,0,4,"ion-item-option",[["color","secondary"]],null,[[null,"click"]],function(n,l,e){var t=!0;return"click"===l&&(t=!1!==n.component.prevStep(u.yb(n,12))&&t),t},i.T,i.p)),u.ob(25,49152,null,0,r.J,[u.h,u.k],{color:[0,"color"],disabled:[1,"disabled"]},null),(n()(),u.pb(26,0,null,0,1,"ion-icon",[["ios","md-arrow-dropleft-circle"],["name","arrow-dropleft-circle"]],null,null,null,i.Q,i.l)),u.ob(27,49152,null,0,r.B,[u.h,u.k],{ios:[0,"ios"],name:[1,"name"]},null),(n()(),u.Fb(-1,0,[" Vorherige Riege "])),(n()(),u.pb(29,0,null,0,6,"ion-item-options",[["side","end"]],null,null,null,i.U,i.q)),u.ob(30,49152,null,0,r.K,[u.h,u.k],{side:[0,"side"]},null),(n()(),u.pb(31,0,null,0,4,"ion-item-option",[["color","secondary"]],null,[[null,"click"]],function(n,l,e){var t=!0;return"click"===l&&(t=!1!==n.component.nextStep(u.yb(n,12))&&t),t},i.T,i.p)),u.ob(32,49152,null,0,r.J,[u.h,u.k],{color:[0,"color"],disabled:[1,"disabled"]},null),(n()(),u.pb(33,0,null,0,1,"ion-icon",[["ios","md-arrow-dropright-circle"],["name","arrow-dropright-circle"]],null,null,null,i.Q,i.l)),u.ob(34,49152,null,0,r.B,[u.h,u.k],{ios:[0,"ios"],name:[1,"name"]},null),(n()(),u.Fb(-1,0,[" N\xe4chste Riege "]))],function(n,l){var e=l.component;n(l,10,0,e.durchgang&&e.geraet),n(l,19,0,e.geraet&&e.getSteps()&&e.getSteps().length>0),n(l,21,0,e.geraet&&e.getSteps()),n(l,23,0,"start"),n(l,25,0,"secondary",!e.geraet||!e.getSteps()),n(l,27,0,"md-arrow-dropleft-circle","arrow-dropleft-circle"),n(l,30,0,"end"),n(l,32,0,"secondary",!e.geraet||!e.getSteps()),n(l,34,0,"md-arrow-dropright-circle","arrow-dropright-circle")},function(n,l){var e=l.component;n(l,6,0,e.competitionName()),n(l,17,0,e.geraet&&e.getSteps()?"Riege ("+e.getSteps()[0]+" bis "+e.getSteps()[e.getSteps().length-1]+")":"Keine Daten")})}function v(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,3,"ion-select-option",[],null,null,null,i.eb,i.A)),u.ob(1,49152,null,0,r.mb,[u.h,u.k],{value:[0,"value"]},null),(n()(),u.Fb(2,0,[" ",""])),u.Bb(3,2)],function(n,l){n(l,1,0,l.context.$implicit.uuid)},function(n,l){var e=l.context.$implicit.titel+" "+u.Gb(l,2,0,n(l,3,0,u.yb(l.parent.parent.parent,0),l.context.$implicit.datum,"dd-MM-yy"));n(l,2,0,e)})}function y(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,8,"ion-select",[["cancelText","Abbrechen"],["okText","Okay"],["placeholder","Bitte ausw\xe4hlen"]],[[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ngModelChange"],[null,"ionBlur"],[null,"ionChange"]],function(n,l,e){var t=!0,o=n.component;return"ionBlur"===l&&(t=!1!==u.yb(n,1)._handleBlurEvent()&&t),"ionChange"===l&&(t=!1!==u.yb(n,1)._handleChangeEvent(e.target.value)&&t),"ngModelChange"===l&&(t=!1!==(o.competition=e)&&t),t},i.fb,i.z)),u.ob(1,16384,null,0,r.Kb,[u.k],null,null),u.Cb(1024,null,a.d,function(n){return[n]},[r.Kb]),u.ob(3,671744,null,0,a.i,[[8,null],[8,null],[8,null],[6,a.d]],{model:[0,"model"]},{update:"ngModelChange"}),u.Cb(2048,null,a.e,null,[a.i]),u.ob(5,16384,null,0,a.f,[[4,a.e]],null,null),u.ob(6,49152,null,0,r.lb,[u.h,u.k],{cancelText:[0,"cancelText"],okText:[1,"okText"],placeholder:[2,"placeholder"]},null),(n()(),u.gb(16777216,null,0,1,null,v)),u.ob(8,278528,null,0,c.l,[u.O,u.L,u.s],{ngForOf:[0,"ngForOf"]},null)],function(n,l){var e=l.component;n(l,3,0,e.competition),n(l,6,0,"Abbrechen","Okay","Bitte ausw\xe4hlen"),n(l,8,0,e.getCompetitions())},function(n,l){n(l,0,0,u.yb(l,5).ngClassUntouched,u.yb(l,5).ngClassTouched,u.yb(l,5).ngClassPristine,u.yb(l,5).ngClassDirty,u.yb(l,5).ngClassValid,u.yb(l,5).ngClassInvalid,u.yb(l,5).ngClassPending)})}function S(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,2,"ion-select-option",[],null,null,null,i.eb,i.A)),u.ob(1,49152,null,0,r.mb,[u.h,u.k],{value:[0,"value"]},null),(n()(),u.Fb(2,0,["",""]))],function(n,l){n(l,1,0,l.context.$implicit)},function(n,l){n(l,2,0,l.context.$implicit)})}function C(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,8,"ion-select",[["cancelText","Abbrechen"],["okText","Okay"],["placeholder","Bitte ausw\xe4hlen"]],[[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ngModelChange"],[null,"ionBlur"],[null,"ionChange"]],function(n,l,e){var t=!0,o=n.component;return"ionBlur"===l&&(t=!1!==u.yb(n,1)._handleBlurEvent()&&t),"ionChange"===l&&(t=!1!==u.yb(n,1)._handleChangeEvent(e.target.value)&&t),"ngModelChange"===l&&(t=!1!==(o.durchgang=e)&&t),t},i.fb,i.z)),u.ob(1,16384,null,0,r.Kb,[u.k],null,null),u.Cb(1024,null,a.d,function(n){return[n]},[r.Kb]),u.ob(3,671744,null,0,a.i,[[8,null],[8,null],[8,null],[6,a.d]],{model:[0,"model"]},{update:"ngModelChange"}),u.Cb(2048,null,a.e,null,[a.i]),u.ob(5,16384,null,0,a.f,[[4,a.e]],null,null),u.ob(6,49152,null,0,r.lb,[u.h,u.k],{cancelText:[0,"cancelText"],okText:[1,"okText"],placeholder:[2,"placeholder"]},null),(n()(),u.gb(16777216,null,0,1,null,S)),u.ob(8,278528,null,0,c.l,[u.O,u.L,u.s],{ngForOf:[0,"ngForOf"]},null)],function(n,l){var e=l.component;n(l,3,0,e.durchgang),n(l,6,0,"Abbrechen","Okay","Bitte ausw\xe4hlen"),n(l,8,0,e.getDurchgaenge())},function(n,l){n(l,0,0,u.yb(l,5).ngClassUntouched,u.yb(l,5).ngClassTouched,u.yb(l,5).ngClassPristine,u.yb(l,5).ngClassDirty,u.yb(l,5).ngClassValid,u.yb(l,5).ngClassInvalid,u.yb(l,5).ngClassPending)})}function w(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,2,"ion-select-option",[],null,null,null,i.eb,i.A)),u.ob(1,49152,null,0,r.mb,[u.h,u.k],{value:[0,"value"]},null),(n()(),u.Fb(2,0,["",""]))],function(n,l){n(l,1,0,l.context.$implicit.id)},function(n,l){n(l,2,0,l.context.$implicit.name)})}function x(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,8,"ion-select",[["cancelText","Abbrechen"],["okText","Okay"],["placeholder","Bitte ausw\xe4hlen"]],[[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ngModelChange"],[null,"ionBlur"],[null,"ionChange"]],function(n,l,e){var t=!0,o=n.component;return"ionBlur"===l&&(t=!1!==u.yb(n,1)._handleBlurEvent()&&t),"ionChange"===l&&(t=!1!==u.yb(n,1)._handleChangeEvent(e.target.value)&&t),"ngModelChange"===l&&(t=!1!==(o.geraet=e)&&t),t},i.fb,i.z)),u.ob(1,16384,null,0,r.Kb,[u.k],null,null),u.Cb(1024,null,a.d,function(n){return[n]},[r.Kb]),u.ob(3,671744,null,0,a.i,[[8,null],[8,null],[8,null],[6,a.d]],{model:[0,"model"]},{update:"ngModelChange"}),u.Cb(2048,null,a.e,null,[a.i]),u.ob(5,16384,null,0,a.f,[[4,a.e]],null,null),u.ob(6,49152,null,0,r.lb,[u.h,u.k],{cancelText:[0,"cancelText"],okText:[1,"okText"],placeholder:[2,"placeholder"]},null),(n()(),u.gb(16777216,null,0,1,null,w)),u.ob(8,278528,null,0,c.l,[u.O,u.L,u.s],{ngForOf:[0,"ngForOf"]},null)],function(n,l){var e=l.component;n(l,3,0,e.geraet),n(l,6,0,"Abbrechen","Okay","Bitte ausw\xe4hlen"),n(l,8,0,e.getGeraete())},function(n,l){n(l,0,0,u.yb(l,5).ngClassUntouched,u.yb(l,5).ngClassTouched,u.yb(l,5).ngClassPristine,u.yb(l,5).ngClassDirty,u.yb(l,5).ngClassValid,u.yb(l,5).ngClassInvalid,u.yb(l,5).ngClassPending)})}function I(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,2,"ion-note",[["slot","end"]],null,null,null,i.bb,i.w)),u.ob(1,49152,null,0,r.W,[u.h,u.k],null,null),(n()(),u.Fb(2,0,[" "," "]))],null,function(n,l){n(l,2,0,"Riege "+l.component.step)})}function O(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,1,"ion-icon",[["name","arrow-dropright"],["slot","end"]],null,null,null,i.Q,i.l)),u.ob(1,49152,null,0,r.B,[u.h,u.k],{name:[0,"name"]},null)],function(n,l){n(l,1,0,"arrow-dropright")},null)}function B(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,47,"ion-list",[],null,null,null,i.Y,i.t)),u.ob(1,49152,null,0,r.N,[u.h,u.k],null,null),(n()(),u.pb(2,0,null,0,6,"ion-item",[],null,null,null,i.W,i.n)),u.ob(3,49152,null,0,r.G,[u.h,u.k],null,null),(n()(),u.pb(4,0,null,0,2,"ion-label",[],null,null,null,i.X,i.s)),u.ob(5,49152,null,0,r.M,[u.h,u.k],null,null),(n()(),u.Fb(-1,0,["Wettkampf"])),(n()(),u.gb(16777216,null,0,1,null,y)),u.ob(8,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(n()(),u.pb(9,0,null,0,6,"ion-item",[],null,null,null,i.W,i.n)),u.ob(10,49152,null,0,r.G,[u.h,u.k],null,null),(n()(),u.pb(11,0,null,0,2,"ion-label",[],null,null,null,i.X,i.s)),u.ob(12,49152,null,0,r.M,[u.h,u.k],null,null),(n()(),u.Fb(-1,0,["Durchgang"])),(n()(),u.gb(16777216,null,0,1,null,C)),u.ob(15,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(n()(),u.pb(16,0,null,0,6,"ion-item",[],null,null,null,i.W,i.n)),u.ob(17,49152,null,0,r.G,[u.h,u.k],null,null),(n()(),u.pb(18,0,null,0,2,"ion-label",[],null,null,null,i.X,i.s)),u.ob(19,49152,null,0,r.M,[u.h,u.k],null,null),(n()(),u.Fb(-1,0,["Ger\xe4t"])),(n()(),u.gb(16777216,null,0,1,null,x)),u.ob(22,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(n()(),u.pb(23,0,null,0,24,"ion-item-sliding",[],null,null,null,i.V,i.r)),u.ob(24,49152,[["slidingStepItem",4]],0,r.L,[u.h,u.k],null,null),(n()(),u.pb(25,0,null,0,8,"ion-item",[],null,[[null,"click"]],function(n,l,e){var t=!0;return"click"===l&&(t=!1!==n.component.itemTapped(u.yb(n,24))&&t),t},i.W,i.n)),u.ob(26,49152,null,0,r.G,[u.h,u.k],null,null),(n()(),u.pb(27,0,null,0,2,"ion-label",[],null,null,null,i.X,i.s)),u.ob(28,49152,null,0,r.M,[u.h,u.k],null,null),(n()(),u.Fb(29,0,["",""])),(n()(),u.gb(16777216,null,0,1,null,I)),u.ob(31,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(n()(),u.gb(16777216,null,0,1,null,O)),u.ob(33,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(n()(),u.pb(34,0,null,0,6,"ion-item-options",[["side","start"]],null,null,null,i.U,i.q)),u.ob(35,49152,null,0,r.K,[u.h,u.k],{side:[0,"side"]},null),(n()(),u.pb(36,0,null,0,4,"ion-item-option",[["color","secondary"]],null,[[null,"click"]],function(n,l,e){var t=!0;return"click"===l&&(t=!1!==n.component.prevStep(u.yb(n,24))&&t),t},i.T,i.p)),u.ob(37,49152,null,0,r.J,[u.h,u.k],{color:[0,"color"],disabled:[1,"disabled"]},null),(n()(),u.pb(38,0,null,0,1,"ion-icon",[["ios","md-arrow-dropleft-circle"],["name","arrow-dropleft-circle"]],null,null,null,i.Q,i.l)),u.ob(39,49152,null,0,r.B,[u.h,u.k],{ios:[0,"ios"],name:[1,"name"]},null),(n()(),u.Fb(-1,0,[" Vorherige Riege "])),(n()(),u.pb(41,0,null,0,6,"ion-item-options",[["side","end"]],null,null,null,i.U,i.q)),u.ob(42,49152,null,0,r.K,[u.h,u.k],{side:[0,"side"]},null),(n()(),u.pb(43,0,null,0,4,"ion-item-option",[["color","secondary"]],null,[[null,"click"]],function(n,l,e){var t=!0;return"click"===l&&(t=!1!==n.component.nextStep(u.yb(n,24))&&t),t},i.T,i.p)),u.ob(44,49152,null,0,r.J,[u.h,u.k],{color:[0,"color"],disabled:[1,"disabled"]},null),(n()(),u.pb(45,0,null,0,1,"ion-icon",[["ios","md-arrow-dropright-circle"],["name","arrow-dropright-circle"]],null,null,null,i.Q,i.l)),u.ob(46,49152,null,0,r.B,[u.h,u.k],{ios:[0,"ios"],name:[1,"name"]},null),(n()(),u.Fb(-1,0,[" N\xe4chste Riege "]))],function(n,l){var e=l.component;n(l,8,0,e.getCompetitions()),n(l,15,0,e.competition&&e.getDurchgaenge()&&e.getDurchgaenge().length>0),n(l,22,0,e.durchgang&&e.getGeraete()&&e.getGeraete().length>0),n(l,31,0,e.geraet&&e.getSteps()&&e.getSteps().length>0),n(l,33,0,e.geraet&&e.getSteps()),n(l,35,0,"start"),n(l,37,0,"secondary",!e.geraet||!e.getSteps()),n(l,39,0,"md-arrow-dropleft-circle","arrow-dropleft-circle"),n(l,42,0,"end"),n(l,44,0,"secondary",!e.geraet||!e.getSteps()),n(l,46,0,"md-arrow-dropright-circle","arrow-dropright-circle")},function(n,l){var e=l.component;n(l,29,0,e.geraet&&e.getSteps()?"Riege ("+e.getSteps()[0]+" bis "+e.getSteps()[e.getSteps().length-1]+")":"Keine Daten")})}function F(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,4,"ion-button",[["color","primary"],["expand","block"],["size","large"]],null,[[null,"click"]],function(n,l,e){var u=!0;return"click"===l&&(u=!1!==n.component.navToStation()&&u),u},i.I,i.d)),u.ob(1,49152,null,0,r.j,[u.h,u.k],{color:[0,"color"],disabled:[1,"disabled"],expand:[2,"expand"],size:[3,"size"]},null),(n()(),u.pb(2,0,null,0,1,"ion-icon",[["name","list"],["slot","start"]],null,null,null,i.Q,i.l)),u.ob(3,49152,null,0,r.B,[u.h,u.k],{name:[0,"name"]},null),(n()(),u.Fb(-1,0,[" Resultate "]))],function(n,l){var e=l.component;n(l,1,0,"primary",!(e.geraet&&e.getSteps()),"block","large"),n(l,3,0,"list")},null)}function T(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,4,"ion-button",[["color","primary"],["expand","block"],["size","large"]],null,[[null,"click"]],function(n,l,e){var u=!0;return"click"===l&&(u=!1!==n.component.navToStation()&&u),u},i.I,i.d)),u.ob(1,49152,null,0,r.j,[u.h,u.k],{color:[0,"color"],disabled:[1,"disabled"],expand:[2,"expand"],size:[3,"size"]},null),(n()(),u.pb(2,0,null,0,1,"ion-icon",[["name","create"],["slot","start"]],null,null,null,i.Q,i.l)),u.ob(3,49152,null,0,r.B,[u.h,u.k],{name:[0,"name"]},null),(n()(),u.Fb(-1,0,[" Resultate "]))],function(n,l){var e=l.component;n(l,1,0,"primary",!(e.geraet&&e.getSteps()),"block","large"),n(l,3,0,"create")},null)}function L(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,6,"ion-list",[],null,null,null,i.Y,i.t)),u.ob(1,49152,null,0,r.N,[u.h,u.k],null,null),(n()(),u.pb(2,0,null,0,4,"ion-button",[["color","warning"],["expand","block"],["size","large"]],null,[[null,"click"]],function(n,l,e){var u=!0;return"click"===l&&(u=!1!==n.component.unlock()&&u),u},i.I,i.d)),u.ob(3,49152,null,0,r.j,[u.h,u.k],{color:[0,"color"],expand:[1,"expand"],size:[2,"size"]},null),(n()(),u.pb(4,0,null,0,1,"ion-icon",[["name","unlock"],["slot","start"]],null,null,null,i.Q,i.l)),u.ob(5,49152,null,0,r.B,[u.h,u.k],{name:[0,"name"]},null),(n()(),u.Fb(-1,0,["Einstellung zur\xfccksetzen "]))],function(n,l){n(l,3,0,"warning","block","large"),n(l,5,0,"unlock")},null)}function A(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,6,"ion-list",[],null,null,null,i.Y,i.t)),u.ob(1,49152,null,0,r.N,[u.h,u.k],null,null),(n()(),u.pb(2,0,null,0,4,"ion-button",[["color","danger"],["expand","block"],["size","large"]],null,[[null,"click"]],function(n,l,e){var u=!0;return"click"===l&&(u=!1!==n.component.logout()&&u),u},i.I,i.d)),u.ob(3,49152,null,0,r.j,[u.h,u.k],{color:[0,"color"],expand:[1,"expand"],size:[2,"size"]},null),(n()(),u.pb(4,0,null,0,1,"ion-icon",[["name","log-out"],["slot","start"]],null,null,null,i.Q,i.l)),u.ob(5,49152,null,0,r.B,[u.h,u.k],{name:[0,"name"]},null),(n()(),u.Fb(-1,0,["Abmelden "]))],function(n,l){n(l,3,0,"danger","block","large"),n(l,5,0,"log-out")},null)}function H(n){return u.Hb(0,[u.zb(0,c.e,[u.u]),(n()(),u.pb(1,0,null,null,14,"ion-header",[],null,null,null,i.P,i.k)),u.ob(2,49152,null,0,r.A,[u.h,u.k],null,null),(n()(),u.pb(3,0,null,0,12,"ion-toolbar",[],null,null,null,i.jb,i.E)),u.ob(4,49152,null,0,r.Ab,[u.h,u.k],null,null),(n()(),u.pb(5,0,null,0,7,"ion-buttons",[["slot","start"]],null,null,null,i.J,i.e)),u.ob(6,49152,null,0,r.k,[u.h,u.k],null,null),(n()(),u.pb(7,0,null,0,5,"ion-menu-toggle",[],null,null,null,i.Z,i.v)),u.ob(8,49152,null,0,r.R,[u.h,u.k],null,null),(n()(),u.pb(9,0,null,0,3,"ion-button",[],null,null,null,i.I,i.d)),u.ob(10,49152,null,0,r.j,[u.h,u.k],null,null),(n()(),u.pb(11,0,null,0,1,"ion-icon",[["name","menu"],["slot","icon-only"]],null,null,null,i.Q,i.l)),u.ob(12,49152,null,0,r.B,[u.h,u.k],{name:[0,"name"]},null),(n()(),u.pb(13,0,null,0,2,"ion-title",[],null,null,null,i.ib,i.D)),u.ob(14,49152,null,0,r.yb,[u.h,u.k],null,null),(n()(),u.Fb(-1,0,["Wettkampf App"])),(n()(),u.pb(16,0,null,null,11,"ion-content",[],null,null,null,i.M,i.h)),u.ob(17,49152,null,0,r.t,[u.h,u.k],null,null),(n()(),u.gb(16777216,null,0,1,null,k)),u.ob(19,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(n()(),u.gb(16777216,null,0,1,null,B)),u.ob(21,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(n()(),u.pb(22,0,null,0,5,"ion-list",[],null,null,null,i.Y,i.t)),u.ob(23,49152,null,0,r.N,[u.h,u.k],null,null),(n()(),u.gb(16777216,null,0,1,null,F)),u.ob(25,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(n()(),u.gb(16777216,null,0,1,null,T)),u.ob(27,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(n()(),u.pb(28,0,null,null,7,"ion-footer",[],null,null,null,i.N,i.i)),u.ob(29,49152,null,0,r.y,[u.h,u.k],null,null),(n()(),u.pb(30,0,null,0,5,"ion-toolbar",[],null,null,null,i.jb,i.E)),u.ob(31,49152,null,0,r.Ab,[u.h,u.k],null,null),(n()(),u.gb(16777216,null,0,1,null,L)),u.ob(33,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null),(n()(),u.gb(16777216,null,0,1,null,A)),u.ob(35,16384,null,0,c.m,[u.O,u.L],{ngIf:[0,"ngIf"]},null)],function(n,l){var e=l.component;n(l,12,0,"menu"),n(l,19,0,e.isLocked),n(l,21,0,!e.isLocked),n(l,25,0,!e.isLoggedIn),n(l,27,0,e.isLoggedIn),n(l,33,0,e.isLocked),n(l,35,0,e.isLoggedIn)},null)}function j(n){return u.Hb(0,[(n()(),u.pb(0,0,null,null,1,"app-page-home",[],null,null,null,H,d)),u.ob(1,114688,null,0,p,[r.Gb,b.a,r.a],null,null)],function(n,l){n(l,1,0)},null)}var N=u.lb("app-page-home",p,j,{},{},[]),M=e("ZYCi");e.d(l,"HomePageModuleNgFactory",function(){return z});var z=u.mb(t,[],function(n){return u.vb([u.wb(512,u.j,u.bb,[[8,[o.a,N]],[3,u.j],u.x]),u.wb(4608,c.o,c.n,[u.u,[2,c.u]]),u.wb(4608,a.n,a.n,[]),u.wb(4608,r.b,r.b,[u.z,u.g]),u.wb(4608,r.Fb,r.Fb,[r.b,u.j,u.q,c.d]),u.wb(4608,r.Jb,r.Jb,[r.b,u.j,u.q,c.d]),u.wb(1073742336,c.c,c.c,[]),u.wb(1073742336,a.l,a.l,[]),u.wb(1073742336,a.b,a.b,[]),u.wb(1073742336,r.Cb,r.Cb,[]),u.wb(1073742336,M.n,M.n,[[2,M.t],[2,M.m]]),u.wb(1073742336,t,t,[]),u.wb(1024,M.k,function(){return[[{path:"",component:p}]]},[])])})}}]);