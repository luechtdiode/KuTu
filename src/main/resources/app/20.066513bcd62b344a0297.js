(window.webpackJsonp=window.webpackJsonp||[]).push([[20],{"+a+d":function(n,e,l){"use strict";l.r(e);var t=l("CcnG"),u=l("mrSG"),i=l("ZZ/e"),o=l("cygB"),r=l("Gyf/"),c=l("67Y/"),a=function(){function n(n,e,l,t){var u=this;this.navCtrl=n,this.backendService=e,this.alertCtrl=l,this.zone=t,this.durchgangopen=!1,this.geraete=[],this.backendService.durchgangStarted.pipe(Object(c.a)(function(n){return n.filter(function(n){return Object(r.c)(n.durchgang)===Object(r.c)(u.backendService.durchgang)&&n.wettkampfUUID===u.backendService.competition}).length>0})).subscribe(function(n){u.durchgangopen=n})}return n.prototype.ionViewWillEnter=function(){this.geraete.length>0&&!this.backendService.captionmode&&this.backendService.activateCaptionMode()},n.prototype.ngOnInit=function(){var n=this;this.backendService.geraeteSubject.subscribe(function(e){n.backendService.captionmode&&(n.geraete=e)})},n.prototype.durchgangstate=function(){return this.backendService.isWebsocketConnected()?this.durchgangopen?"gestartet":"gesperrt":"offline"},Object.defineProperty(n.prototype,"durchgangstateClass",{get:function(){return{open:this.backendService.isWebsocketConnected()&&this.durchgangopen,closed:this.backendService.isWebsocketConnected()&&!this.durchgangopen,offline:!this.backendService.isWebsocketConnected()}},enumerable:!0,configurable:!0}),Object.defineProperty(n.prototype,"competition",{get:function(){return this.backendService.competition},set:function(n){this.stationFreezed||this.backendService.getDurchgaenge(n)},enumerable:!0,configurable:!0}),Object.defineProperty(n.prototype,"durchgang",{get:function(){return this.backendService.durchgang},set:function(n){this.stationFreezed||this.backendService.getGeraete(this.competition,n)},enumerable:!0,configurable:!0}),Object.defineProperty(n.prototype,"geraet",{get:function(){return this.backendService.geraet},set:function(n){this.stationFreezed||this.backendService.getSteps(this.competition,this.durchgang,n)},enumerable:!0,configurable:!0}),Object.defineProperty(n.prototype,"step",{get:function(){return this.backendService.step},set:function(n){this.backendService.getWertungen(this.competition,this.durchgang,this.geraet,n)},enumerable:!0,configurable:!0}),Object.defineProperty(n.prototype,"stationFreezed",{get:function(){return this.backendService.stationFreezed},enumerable:!0,configurable:!0}),n.prototype.isLoggedIn=function(){return this.backendService.loggedIn},Object.defineProperty(n.prototype,"nextStepCaption",{get:function(){return this.isLoggedIn()?"N\xe4chste Riege":"N\xe4chstes Ger\xe4t"},enumerable:!0,configurable:!0}),Object.defineProperty(n.prototype,"prevStepCaption",{get:function(){return this.isLoggedIn()?"Vorherige Riege":"Vorheriges Ger\xe4t"},enumerable:!0,configurable:!0}),n.prototype.itemTapped=function(n){n.getOpenAmount().then(function(e){e>10||e<-10?n.close():n.open("end")})},n.prototype.nextStep=function(n){var e=this;this.backendService.nextGeraet().subscribe(function(l){e.step=l,n.close()})},n.prototype.prevStep=function(n){var e=this;this.backendService.prevGeraet().subscribe(function(l){e.step=l,n.close()})},Object.defineProperty(n.prototype,"station",{get:function(){return this.isLoggedIn()?this.step+". Riege "+this.geraetName()+", "+this.durchgang:this.step+". Ger\xe4t "+this.geraetName()+", "+this.durchgang},enumerable:!0,configurable:!0}),n.prototype.showCompleteAlert=function(){return u.b(this,void 0,void 0,function(){var n,e=this;return u.e(this,function(l){return(n=this.alertCtrl.create({header:"Achtung",message:'Nach dem Abschliessen der Station "'+this.station+'" k\xf6nnen die erfassten Wertungen nur noch im Wettkampf-B\xfcro korrigiert werden.',buttons:[{text:"Abbrechen",role:"cancel",handler:function(){}},{text:"OKAY",handler:function(){var l=n.then(function(n){return n.dismiss()});return e.zone.run(function(){e.backendService.finishStation(e.competition,e.durchgang,e.geraet,e.step).subscribe(function(n){0===n.length&&l.then(function(){e.navCtrl.pop()})})}),!1}}]})).then(function(n){return n.present()}),[2]})})},n.prototype.toastMissingResult=function(n){return u.b(this,void 0,void 0,function(){var e,l=this;return u.e(this,function(t){switch(t.label){case 0:return e=n[0].vorname.toUpperCase()+" "+n[0].name.toUpperCase()+" ("+n[0].verein+")",[4,this.alertCtrl.create({header:"Achtung",subHeader:"Fehlendes Resultat!",message:"In dieser Riege gibt es noch "+n.length+" leere Wertungen! - Bitte pr\xfcfen, ob "+e+" geturnt hat.",buttons:[{text:"Nicht geturnt",role:"edit",handler:function(){n.splice(0,1),n.length>0?l.toastMissingResult(n):l.showCompleteAlert()}},{text:"Korrigieren",role:"cancel",handler:function(){l.navCtrl.navigateForward("wertung-editor/"+n[0].id)}}]})];case 1:return t.sent().present(),[2]}})})},n.prototype.finish=function(){var n=this.backendService.wertungen.filter(function(n){return void 0===n.wertung.endnote});0!==n.length?this.toastMissingResult(n):this.showCompleteAlert()},n.prototype.getCompetitions=function(){return this.backendService.competitions},n.prototype.competitionName=function(){var n=this;if(!this.backendService.competitions)return"";var e=this.backendService.competitions.filter(function(e){return e.uuid===n.backendService.competition}).map(function(n){return n.titel+"<br>"+(n.datum+"T").split("T")[0].split("-").reverse().join("-")});return 1===e.length?e[0]:""},n.prototype.geraetName=function(){var n=this;if(!this.geraete||0===this.geraete.length)return"";var e=this.geraete.filter(function(e){return e.id===n.backendService.geraet}).map(function(n){return n.name});return 1===e.length?e[0]:""},n.prototype.getDurchgaenge=function(){return this.backendService.durchgaenge},n.prototype.getGeraete=function(){return this.geraete},n.prototype.getSteps=function(){return this.backendService.steps},n.prototype.getWertungen=function(){return this.backendService.wertungenSubject},n}(),s=function(){return function(){}}(),b=l("pMnS"),p=l("oBZk"),g=l("Ip0R"),d=function(){function n(n,e){this.navCtrl=n,this.backendService=e}return n.prototype.itemTapped=function(n,e){this.navCtrl.navigateForward(this.backendService.loggedIn?"wertung-editor/"+e.id:"athlet-view/"+this.backendService.competition+"/"+e.id)},n}(),f=t.nb({encapsulation:0,styles:[[".result-value[_ngcontent-%COMP%]{font-size:large;font-weight:700;margin-top:17px;margin-left:0;padding-left:0;padding-top:0;color:var(--ion-color-success)}.result-no-value[_ngcontent-%COMP%]{font-size:large;font-weight:700;margin-top:17px;margin-left:0;padding-left:0;padding-top:0;color:var(--ion-color-warning)}"]],data:{}});function h(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,3,"ion-note",[["class","result-value"],["slot","end"]],null,null,null,p.bb,p.w)),t.ob(1,49152,null,0,i.W,[t.h,t.k],null,null),(n()(),t.Fb(2,0,[" "," "])),t.Bb(3,2)],null,function(n,e){var l=t.Gb(e,2,0,n(e,3,0,t.yb(e.parent.parent.parent,0),e.parent.context.$implicit.wertung.endnote,"2.3-3"));n(e,2,0,l)})}function m(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,2,"ion-note",[["class","result-no-value"],["slot","end"]],null,null,null,p.bb,p.w)),t.ob(1,49152,null,0,i.W,[t.h,t.k],null,null),(n()(),t.Fb(-1,0,["Kein Resultat"]))],null,null)}function v(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,13,"ion-item",[],null,[[null,"click"]],function(n,e,l){var t=!0;return"click"===e&&(t=!1!==n.component.itemTapped(l,n.context.$implicit)&&t),t},p.W,p.n)),t.ob(1,49152,null,0,i.G,[t.h,t.k],null,null),(n()(),t.pb(2,0,null,0,2,"ion-avatar",[["slot","start"]],null,null,null,p.G,p.b)),t.ob(3,49152,null,0,i.e,[t.h,t.k],null,null),(n()(),t.pb(4,0,null,0,0,"img",[["src","assets/imgs/athlete.png"]],null,null,null,null,null)),(n()(),t.pb(5,0,null,0,4,"ion-label",[],null,null,null,p.X,p.s)),t.ob(6,49152,null,0,i.M,[t.h,t.k],null,null),(n()(),t.Fb(7,0,[""," "])),(n()(),t.pb(8,0,null,0,1,"small",[],null,null,null,null,null)),(n()(),t.Fb(9,null,["",""])),(n()(),t.gb(16777216,null,0,1,null,h)),t.ob(11,16384,null,0,g.m,[t.O,t.L],{ngIf:[0,"ngIf"]},null),(n()(),t.gb(16777216,null,0,1,null,m)),t.ob(13,16384,null,0,g.m,[t.O,t.L],{ngIf:[0,"ngIf"]},null)],function(n,e){n(e,11,0,void 0!==e.context.$implicit.wertung.endnote),n(e,13,0,void 0===e.context.$implicit.wertung.endnote)},function(n,e){n(e,7,0,e.context.$implicit.vorname+" "+e.context.$implicit.name),n(e,9,0," ("+e.context.$implicit.wertung.riege+")")})}function k(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,3,"ion-list",[],null,null,null,p.Y,p.t)),t.ob(1,49152,null,0,i.N,[t.h,t.k],null,null),(n()(),t.gb(16777216,null,0,1,null,v)),t.ob(3,278528,null,0,g.l,[t.O,t.L,t.s],{ngForOf:[0,"ngForOf"]},null)],function(n,e){n(e,3,0,e.component.items)},null)}function S(n){return t.Hb(0,[t.zb(0,g.f,[t.u]),(n()(),t.gb(16777216,null,null,1,null,k)),t.ob(2,16384,null,0,g.m,[t.O,t.L],{ngIf:[0,"ngIf"]},null)],function(n,e){var l=e.component;n(e,2,0,l.items&&l.items.length>0)},null)}var w=t.nb({encapsulation:0,styles:[[".stateinfo.closed[_ngcontent-%COMP%]{padding-left:5px;border-left:6px solid var(--ion-color-danger)}.stateinfo.open[_ngcontent-%COMP%]{padding-left:5px;border-left:6px solid var(--ion-color-success)}.stateinfo.offline[_ngcontent-%COMP%]{padding-left:5px;border-left:6px solid var(--ion-color-warning)}"]],data:{}});function y(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,3,"div",[],null,null,null,null,null)),(n()(),t.pb(1,0,null,null,2,"ion-title",[],null,null,null,p.ib,p.D)),t.ob(2,49152,null,0,i.yb,[t.h,t.k],null,null),(n()(),t.Fb(-1,0,["Resultate"]))],null,null)}function C(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,6,"div",[],null,null,null,null,null)),(n()(),t.pb(1,0,null,null,5,"ion-title",[],null,null,null,p.ib,p.D)),t.ob(2,49152,null,0,i.yb,[t.h,t.k],null,null),(n()(),t.Fb(-1,0,["Resultaterfassung"])),(n()(),t.pb(4,0,null,0,2,"div",[["class","stateinfo"]],null,null,null,null,null)),t.ob(5,278528,null,0,g.k,[t.s,t.t,t.k,t.D],{klass:[0,"klass"],ngClass:[1,"ngClass"]},null),(n()(),t.Fb(6,null,["",""]))],function(n,e){n(e,5,0,"stateinfo",e.component.durchgangstateClass)},function(n,e){n(e,6,0,e.component.durchgangstate())})}function I(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,2,"riege-list",[],null,null,null,S,f)),t.ob(1,49152,null,0,d,[i.Gb,o.a],{items:[0,"items"]},null),t.zb(131072,g.b,[t.h])],function(n,e){var l=e.component;n(e,1,0,t.Gb(e,1,0,t.yb(e,2).transform(l.getWertungen())))},null)}function O(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,2,"ion-note",[["slot","end"]],null,null,null,p.bb,p.w)),t.ob(1,49152,null,0,i.W,[t.h,t.k],null,null),(n()(),t.Fb(2,0,["("," bis ",")"]))],null,function(n,e){var l=e.component;n(e,2,0,l.getSteps()[0],l.getSteps()[l.getSteps().length-1])})}function x(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,1,"ion-icon",[["name","arrow-dropright"],["slot","end"]],null,null,null,p.Q,p.l)),t.ob(1,49152,null,0,i.B,[t.h,t.k],{name:[0,"name"]},null)],function(n,e){n(e,1,0,"arrow-dropright")},null)}function F(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,4,"ion-button",[["color","success"],["expand","block"],["size","large"]],null,[[null,"click"]],function(n,e,l){var t=!0;return"click"===e&&(t=!1!==n.component.finish()&&t),t},p.I,p.d)),t.ob(1,49152,null,0,i.j,[t.h,t.k],{color:[0,"color"],disabled:[1,"disabled"],expand:[2,"expand"],size:[3,"size"]},null),(n()(),t.pb(2,0,null,0,1,"ion-icon",[["name","git-commit"],["slot","start"]],null,null,null,p.Q,p.l)),t.ob(3,49152,null,0,i.B,[t.h,t.k],{name:[0,"name"]},null),(n()(),t.Fb(-1,0,[" Eingaben abschliessen "]))],function(n,e){var l=e.component;n(e,1,0,"success",!(l.geraet&&l.getSteps()),"block","large"),n(e,3,0,"git-commit")},null)}function j(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,14,"ion-header",[],null,null,null,p.P,p.k)),t.ob(1,49152,null,0,i.A,[t.h,t.k],null,null),(n()(),t.pb(2,0,null,0,12,"ion-toolbar",[],null,null,null,p.jb,p.E)),t.ob(3,49152,null,0,i.Ab,[t.h,t.k],null,null),(n()(),t.pb(4,0,null,0,4,"ion-buttons",[["slot","start"]],null,null,null,p.J,p.e)),t.ob(5,49152,null,0,i.k,[t.h,t.k],null,null),(n()(),t.pb(6,0,null,0,2,"ion-back-button",[["defaultHref","/"]],null,[[null,"click"]],function(n,e,l){var u=!0;return"click"===e&&(u=!1!==t.yb(n,8).onClick(l)&&u),u},p.H,p.c)),t.ob(7,49152,null,0,i.f,[t.h,t.k],{defaultHref:[0,"defaultHref"]},null),t.ob(8,16384,null,0,i.g,[[2,i.gb],i.Gb],{defaultHref:[0,"defaultHref"]},null),(n()(),t.gb(16777216,null,0,1,null,y)),t.ob(10,16384,null,0,g.m,[t.O,t.L],{ngIf:[0,"ngIf"]},null),(n()(),t.gb(16777216,null,0,1,null,C)),t.ob(12,16384,null,0,g.m,[t.O,t.L],{ngIf:[0,"ngIf"]},null),(n()(),t.pb(13,0,null,0,1,"ion-note",[["class","ion-padding-end"],["slot","end"]],[[8,"innerHTML",1]],null,null,p.bb,p.w)),t.ob(14,49152,null,0,i.W,[t.h,t.k],null,null),(n()(),t.pb(15,0,null,null,4,"ion-content",[],null,null,null,p.M,p.h)),t.ob(16,49152,null,0,i.t,[t.h,t.k],null,null),(n()(),t.gb(16777216,null,0,2,null,I)),t.ob(18,16384,null,0,g.m,[t.O,t.L],{ngIf:[0,"ngIf"]},null),t.zb(131072,g.b,[t.h]),(n()(),t.pb(20,0,null,null,32,"ion-footer",[],null,null,null,p.N,p.i)),t.ob(21,49152,null,0,i.y,[t.h,t.k],null,null),(n()(),t.pb(22,0,null,0,30,"ion-toolbar",[],null,null,null,p.jb,p.E)),t.ob(23,49152,null,0,i.Ab,[t.h,t.k],null,null),(n()(),t.pb(24,0,null,0,28,"ion-list",[],null,null,null,p.Y,p.t)),t.ob(25,49152,null,0,i.N,[t.h,t.k],null,null),(n()(),t.pb(26,0,null,0,24,"ion-item-sliding",[],null,null,null,p.V,p.r)),t.ob(27,49152,[["slidingStepItem",4]],0,i.L,[t.h,t.k],null,null),(n()(),t.pb(28,0,null,0,8,"ion-item",[],null,[[null,"click"]],function(n,e,l){var u=!0;return"click"===e&&(u=!1!==n.component.itemTapped(t.yb(n,27))&&u),u},p.W,p.n)),t.ob(29,49152,null,0,i.G,[t.h,t.k],null,null),(n()(),t.pb(30,0,null,0,2,"ion-label",[],null,null,null,p.X,p.s)),t.ob(31,49152,null,0,i.M,[t.h,t.k],null,null),(n()(),t.Fb(32,0,["",""])),(n()(),t.gb(16777216,null,0,1,null,O)),t.ob(34,16384,null,0,g.m,[t.O,t.L],{ngIf:[0,"ngIf"]},null),(n()(),t.gb(16777216,null,0,1,null,x)),t.ob(36,16384,null,0,g.m,[t.O,t.L],{ngIf:[0,"ngIf"]},null),(n()(),t.pb(37,0,null,0,6,"ion-item-options",[["side","start"]],null,null,null,p.U,p.q)),t.ob(38,49152,null,0,i.K,[t.h,t.k],{side:[0,"side"]},null),(n()(),t.pb(39,0,null,0,4,"ion-item-option",[["color","primary"]],null,[[null,"click"]],function(n,e,l){var u=!0;return"click"===e&&(u=!1!==n.component.prevStep(t.yb(n,27))&&u),u},p.T,p.p)),t.ob(40,49152,null,0,i.J,[t.h,t.k],{color:[0,"color"],disabled:[1,"disabled"]},null),(n()(),t.pb(41,0,null,0,1,"ion-icon",[["ios","md-arrow-dropleft-circle"],["name","arrow-dropleft-circle"]],null,null,null,p.Q,p.l)),t.ob(42,49152,null,0,i.B,[t.h,t.k],{ios:[0,"ios"],name:[1,"name"]},null),(n()(),t.Fb(43,0,[" "," "])),(n()(),t.pb(44,0,null,0,6,"ion-item-options",[["side","end"]],null,null,null,p.U,p.q)),t.ob(45,49152,null,0,i.K,[t.h,t.k],{side:[0,"side"]},null),(n()(),t.pb(46,0,null,0,4,"ion-item-option",[["color","primary"]],null,[[null,"click"]],function(n,e,l){var u=!0;return"click"===e&&(u=!1!==n.component.nextStep(t.yb(n,27))&&u),u},p.T,p.p)),t.ob(47,49152,null,0,i.J,[t.h,t.k],{color:[0,"color"],disabled:[1,"disabled"]},null),(n()(),t.pb(48,0,null,0,1,"ion-icon",[["ios","md-arrow-dropright-circle"],["name","arrow-dropright-circle"]],null,null,null,p.Q,p.l)),t.ob(49,49152,null,0,i.B,[t.h,t.k],{ios:[0,"ios"],name:[1,"name"]},null),(n()(),t.Fb(50,0,[" "," "])),(n()(),t.gb(16777216,null,0,1,null,F)),t.ob(52,16384,null,0,g.m,[t.O,t.L],{ngIf:[0,"ngIf"]},null)],function(n,e){var l=e.component;n(e,7,0,"/"),n(e,8,0,"/"),n(e,10,0,!l.isLoggedIn()),n(e,12,0,l.isLoggedIn()),n(e,18,0,l.geraet&&l.durchgang&&l.step&&t.Gb(e,18,0,t.yb(e,19).transform(l.getWertungen()))),n(e,34,0,l.getSteps()&&l.getSteps().length>0),n(e,36,0,l.geraet&&l.getSteps()),n(e,38,0,"start"),n(e,40,0,"primary",!l.geraet||!l.getSteps()),n(e,42,0,"md-arrow-dropleft-circle","arrow-dropleft-circle"),n(e,45,0,"end"),n(e,47,0,"primary",!l.geraet||!l.getSteps()),n(e,49,0,"md-arrow-dropright-circle","arrow-dropright-circle"),n(e,52,0,l.isLoggedIn()&&l.durchgangopen)},function(n,e){var l=e.component;n(e,13,0,l.competitionName()),n(e,32,0,l.station),n(e,43,0,l.prevStepCaption),n(e,50,0,l.nextStepCaption)})}function H(n){return t.Hb(0,[(n()(),t.pb(0,0,null,null,1,"app-station",[],null,null,null,j,w)),t.ob(1,114688,null,0,a,[i.Gb,o.a,i.a,t.z],null,null)],function(n,e){n(e,1,0)},null)}var G=t.lb("app-station",a,H,{},{},[]),L=l("gIcY"),W=l("Zr1d"),z=l("ZYCi"),P=l("bDjW");l.d(e,"StationPageModuleNgFactory",function(){return M});var M=t.mb(s,[],function(n){return t.vb([t.wb(512,t.j,t.bb,[[8,[b.a,G]],[3,t.j],t.x]),t.wb(4608,g.o,g.n,[t.u,[2,g.u]]),t.wb(4608,L.m,L.m,[]),t.wb(4608,i.b,i.b,[t.z,t.g]),t.wb(4608,i.Fb,i.Fb,[i.b,t.j,t.q,g.d]),t.wb(4608,i.Jb,i.Jb,[i.b,t.j,t.q,g.d]),t.wb(4608,W.a,W.a,[]),t.wb(1073742336,g.c,g.c,[]),t.wb(1073742336,L.k,L.k,[]),t.wb(1073742336,L.b,L.b,[]),t.wb(1073742336,i.Cb,i.Cb,[]),t.wb(1073742336,z.n,z.n,[[2,z.t],[2,z.m]]),t.wb(1073742336,P.a,P.a,[]),t.wb(1073742336,s,s,[]),t.wb(1024,z.k,function(){return[[{path:"",component:a}]]},[])])})}}]);