from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.popup import Popup
from kivy.properties import ObjectProperty

from atmolib import allcalc


class About(Popup):
    pass


class atmoCalc(BoxLayout):
    h_input = ObjectProperty(None)
    res_output = ObjectProperty(None)
    focus = False

    def get_about(self):
        about = About()
        about.open()

    def change_color(self, ref):
        name = ref.split()[0]
        tmp = []
        for e in self.calc_res:
            if e[0] == name:
                tmp.append(
                    f'{e[0]} = [color=ff0000][ref={e[0]} {e[1]}]{e[1]}[/ref][/color] &bl;{e[2]}&br;\n'
                )
                continue
            tmp.append(
                f'{e[0]} = [color=ffffff][ref={e[0]} {e[1]}]{e[1]}[/ref][/color] &bl;{e[2]}&br;\n'
            )
        self.res_output.text = ''.join(tmp)

    def calc(self):
        self.calc_res = allcalc(self.h_input.text)
        if isinstance(self.calc_res, str):
            self.res_output.text = self.calc_res
            return
        self.res_output.text = ''.join(
            [
                f'{e[0]} = [color=ffffff][ref={e[0]} {e[1]}]{e[1]}[/ref][/color] &bl;{e[2]}&br;\n'
                for e in self.calc_res
            ]
        )


class atmoApp(App):
    def build(self):
        atmosphere_parameters = atmoCalc()
        return atmosphere_parameters


if __name__ == '__main__':
    atmoApp().run()
